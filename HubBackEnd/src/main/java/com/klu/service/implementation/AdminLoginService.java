package com.klu.service.implementation;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.klu.dto.AdminLoginResponseDto;
import com.klu.mail.Login;
import com.klu.mail.UserSignUp;
import com.klu.mail.UserSignUpRepository;
import com.klu.security.JwtService;
import com.klu.security.ratelimit.AdminLoginRateLimiterService;

@Service
public class AdminLoginService {

    private static final String INVALID_CREDENTIALS = "Invalid admin credentials";
    private static final String LOCKED_MESSAGE = "Too many login attempts. Please try again later.";

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserSignUpRepository userRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AdminLoginRateLimiterService rateLimiter;

    /**
     * Serializing admin login decisions in this JVM prevents simultaneous
     * password guesses from racing past the in-memory attempt threshold.
     * Admin access is low volume; use shared atomic rate limiting for scaling.
     */
    public synchronized AdminLoginResponseDto login(Login request) {
        if (request == null || request.getMail() == null
                || request.getMail().isBlank() || request.getPassword() == null
                || request.getPassword().isBlank()) {
            return failure(INVALID_CREDENTIALS);
        }

        String mail = request.getMail().trim().toLowerCase(Locale.ROOT);
        if (rateLimiter.isBlocked(mail)) {
            return failure(LOCKED_MESSAGE);
        }

        // Account existence, user role, and verification state are deliberately
        // indistinguishable to callers, including invalid admin usernames.
        UserSignUp user = userRepo.findByMail(mail).orElse(null);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole()) || !user.isVerified()) {
            return rejectedAttempt(mail);
        }

        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(mail, request.getPassword()));

            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getAuthorities().stream()
                            .noneMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()))) {
                return rejectedAttempt(mail);
            }

            rateLimiter.recordSuccess(mail);
            String token = jwtService.generateToken(authentication.getName());
            return new AdminLoginResponseDto(
                    "Welcome To Admin Dashboard",
                    user.getName(),
                    user.getMail(),
                    token);
        } catch (AuthenticationException exception) {
            return rejectedAttempt(mail);
        }
    }

    public long retryAfterSeconds(String mail) {
        return rateLimiter.retryAfterSeconds(mail);
    }

    private AdminLoginResponseDto rejectedAttempt(String mail) {
        rateLimiter.recordFailure(mail);
        return failure(rateLimiter.isBlocked(mail) ? LOCKED_MESSAGE : INVALID_CREDENTIALS);
    }

    private AdminLoginResponseDto failure(String message) {
        return new AdminLoginResponseDto(message, null, null, null);
    }
}
