package com.klu.service.implementation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.klu.dto.AdminLoginResponseDto;
import com.klu.mail.Login;
import com.klu.mail.UserSignUp;
import com.klu.mail.UserSignUpRepository;
import com.klu.security.JwtService;
import com.klu.security.ratelimit.AdminLoginRateLimiterService;

class AdminLoginServiceTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserSignUpRepository userRepo;

    @Mock
    private JwtService jwtService;

    private AdminLoginService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new AdminLoginService();
        ReflectionTestUtils.setField(service, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(service, "userRepo", userRepo);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        ReflectionTestUtils.setField(service, "rateLimiter", new AdminLoginRateLimiterService());
    }

    private UserSignUp admin() {
        UserSignUp user = new UserSignUp();
        user.setName("Admin");
        user.setMail("admin@example.com");
        user.setRole("ROLE_ADMIN");
        user.setVerified(true);
        return user;
    }

    @Test
    void hidesAccountExistenceAndRoleFromUnauthorizedCallers() {
        when(userRepo.findByMail("unknown@example.com")).thenReturn(Optional.empty());
        AdminLoginResponseDto missing =
                service.login(new Login("unknown@example.com", "bad-password"));
        assertEquals("Invalid admin credentials", missing.getMessage());
        assertNull(missing.getToken());

        UserSignUp student = admin();
        student.setRole("ROLE_STUDENT");
        when(userRepo.findByMail("admin@example.com")).thenReturn(Optional.of(student));
        AdminLoginResponseDto nonAdmin =
                service.login(new Login("admin@example.com", "bad-password"));
        assertEquals(missing.getMessage(), nonAdmin.getMessage());
        assertNull(nonAdmin.getToken());

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void fifthBadPasswordLocksAccountAndDoesNotAuthenticateAfterLock() {
        when(userRepo.findByMail("admin@example.com")).thenReturn(Optional.of(admin()));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        for (int i = 0; i < 4; i++) {
            AdminLoginResponseDto response =
                    service.login(new Login("ADMIN@EXAMPLE.COM", "incorrect"));
            assertEquals("Invalid admin credentials", response.getMessage());
        }

        AdminLoginResponseDto fifth =
                service.login(new Login("admin@example.com", "incorrect"));
        assertTrue(fifth.getMessage().startsWith("Too many login attempts"));
        assertNull(fifth.getToken());

        AdminLoginResponseDto blocked =
                service.login(new Login("admin@example.com", "even-if-correct"));
        assertEquals(fifth.getMessage(), blocked.getMessage());
        assertTrue(service.retryAfterSeconds("admin@example.com") > 0L);
        verify(authenticationManager, times(5)).authenticate(any());
    }

    @Test
    void successfulAdminAuthenticationReturnsTokenAndClearsFailures() {
        when(userRepo.findByMail("admin@example.com")).thenReturn(Optional.of(admin()));
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("admin@example.com")).thenReturn("test-jwt");

        AdminLoginResponseDto result =
                service.login(new Login(" ADMIN@EXAMPLE.COM ", "correct-password"));

        assertEquals("test-jwt", result.getToken());
        assertEquals("Admin", result.getName());
        assertEquals("admin@example.com", result.getEmail());
        assertEquals(0L, service.retryAfterSeconds("admin@example.com"));
    }

    @Test
    void authenticatedNonAdminNeverReceivesAnAdminToken() {
        when(userRepo.findByMail("admin@example.com")).thenReturn(Optional.of(admin()));
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        AdminLoginResponseDto response =
                service.login(new Login("admin@example.com", "correct-password"));

        assertNull(response.getToken());
        assertEquals("Invalid admin credentials", response.getMessage());
        verifyNoInteractions(jwtService);
    }

    @Test
    void emptyCredentialsAreRejectedWithoutDatabaseLookup() {
        AdminLoginResponseDto result = service.login(new Login("  ", ""));
        assertNull(result.getToken());
        assertEquals("Invalid admin credentials", result.getMessage());
        verifyNoInteractions(userRepo, authenticationManager, jwtService);
    }
}
