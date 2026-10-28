package com.klu.security;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.klu.mail.UserSignUp;

/**
 * Shared verification rules for signup and password reset.
 * The OTP state and failed-attempt counter are persisted with the account,
 * so a restart does not reset the guessing budget.
 */
@Service
public class OtpSecurityService {
    public enum Purpose {
        STUDENT_SIGNUP, REVIEWER_SIGNUP, PASSWORD_RESET
    }

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int OTP_TTL_MINUTES = 3;
    private final SecureRandom random = new SecureRandom();

    public int issue(UserSignUp user, Purpose purpose) {
        int otp = 100000 + random.nextInt(900000);
        user.setOtp(otp);
        user.setOtpPurpose(purpose.name());
        user.setOtpTimeOut(LocalDateTime.now());
        user.setOtpFailedAttempts(0);
        user.setResetOtpVerified(false);
        return otp;
    }

    /**
     * Returns null on success; otherwise returns a user-safe failure message.
     * Caller must save the account even on failures so counters remain durable.
     */
    public String check(UserSignUp user, int supplied, Purpose purpose) {
        if (user == null || user.getOtpTimeOut() == null
                || user.getOtp() < 100000
                || !purpose.name().equals(user.getOtpPurpose())
                || !LocalDateTime.now().isBefore(user.getOtpTimeOut().plusMinutes(OTP_TTL_MINUTES))) {
            return "OTP Expired";
        }
        if (user.getOtpFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return "Too many invalid OTP attempts. Request a new OTP.";
        }
        if (user.getOtp() != supplied) {
            user.setOtpFailedAttempts(user.getOtpFailedAttempts() + 1);
            if (user.getOtpFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                invalidate(user);
                return "Too many invalid OTP attempts. Request a new OTP.";
            }
            return "Invalid OTP";
        }
        return null;
    }

    public void verifiedForPasswordReset(UserSignUp user) {
        user.setResetOtpVerified(true);
        user.setOtp(0);
        user.setOtpTimeOut(LocalDateTime.now());
        user.setOtpFailedAttempts(0);
        // Preserve the PASSWORD_RESET purpose until the verified grant expires.
    }

    public boolean canResetPassword(UserSignUp user) {
        return user != null && user.isResetOtpVerified()
                && Purpose.PASSWORD_RESET.name().equals(user.getOtpPurpose())
                && user.getOtpTimeOut() != null
                && LocalDateTime.now().isBefore(user.getOtpTimeOut().plusMinutes(OTP_TTL_MINUTES));
    }

    public void invalidate(UserSignUp user) {
        user.setOtp(0);
        user.setOtpPurpose(null);
        user.setOtpTimeOut(null);
        user.setResetOtpVerified(false);
    }
}
