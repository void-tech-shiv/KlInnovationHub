package com.klu.service.implementation;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klu.dto.ResetPasswordDto;
import com.klu.dto.VerifyResetOtpDto;
import com.klu.mail.UserSignUp;
import com.klu.mail.UserSignUpRepository;
import com.klu.model.Reviewer;
import com.klu.repository.ReviewerRepo;
import com.klu.security.OtpSecurityService;
import com.klu.security.OtpSecurityService.Purpose;
import com.klu.security.ratelimit.PasswordResetRateLimiterService;

@Service
public class ReviewerPasswordService {

    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,64}$";

    @Autowired
    private UserSignUpRepository userRepo;

    @Autowired
    private ReviewerRepo reviewerRepo;

    @Autowired
    private JavaMailSender sender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetRateLimiterService passwordResetRateLimiterService;

    @Autowired private OtpSecurityService otpSecurity;

    @Transactional
    public String forgotPassword(String mail) {
        UserSignUp user = mail == null ? null : userRepo.findLockedByMail(mail).orElse(null);
        if (user == null || !"ROLE_REVIEWER".equals(user.getRole())) {
            return "Reviewer account not found";
        }
        if (!user.isVerified()) {
            return "Please verify your email first";
        }

        Reviewer reviewer = reviewerRepo.findByUserMail(mail).orElse(null);
        if (reviewer == null) {
            return "Your reviewer account is not approved";
        }

        PasswordResetRateLimiterService.RateLimitResult rateLimit =
                passwordResetRateLimiterService.tryRequest(mail);
        if (!rateLimit.isAllowed()) {
            return "Too many password reset requests. Please try again later.";
        }

        int otp = otpSecurity.issue(user, Purpose.PASSWORD_RESET);
        userRepo.save(user);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mail);
        message.setSubject("Reset Your Reviewer Password • KL Innovation Hub");
        message.setText(
                "Hello " + user.getName() + ",\n\n" +
                "We received a request to reset your KL Innovation Hub reviewer password.\n\n" +
                "OTP : " + otp + "\n\n" +
                "This OTP is valid for 3 minutes. If you did not request this, you can ignore this email.\n\n" +
                "— KL Innovation Hub");
        sender.send(message);
        return "OTP Sent";
    }

    @Transactional
    public String verifyResetOtp(VerifyResetOtpDto request) {
        String mail = request.getMail();
        UserSignUp user = mail == null ? null : userRepo.findLockedByMail(mail).orElse(null);
        if (user == null || !"ROLE_REVIEWER".equals(user.getRole()) || !user.isVerified()) {
            return "Reviewer account not found";
        }
        if (reviewerRepo.findByUserMail(mail).isEmpty()) {
            return "Your reviewer account is not approved";
        }
        String error = otpSecurity.check(user, request.getOtp(), Purpose.PASSWORD_RESET);
        if (error != null) {
            userRepo.save(user);
            return error;
        }
        otpSecurity.verifiedForPasswordReset(user);
        userRepo.save(user);
        return "OTP Verified";
    }

    @Transactional
    public String resetPassword(ResetPasswordDto request) {
        String mail = request.getMail();
        UserSignUp user = mail == null ? null : userRepo.findLockedByMail(mail).orElse(null);
        if (user == null || !"ROLE_REVIEWER".equals(user.getRole()) || !user.isVerified()) {
            return "Reviewer account not found";
        }
        if (reviewerRepo.findByUserMail(mail).isEmpty()) {
            return "Your reviewer account is not approved";
        }
        if (request.getNewPassword() == null || !request.getNewPassword().matches(PASSWORD_REGEX)) {
            return "Password must contain uppercase, lowercase, number, symbol and be at least 10 characters long";
        }
        if (!otpSecurity.canResetPassword(user)) {
            return "Verify OTP First";
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        otpSecurity.invalidate(user);
        userRepo.save(user);
        return "Password Updated Successfully";
    }
}
