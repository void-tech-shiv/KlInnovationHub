package com.klu.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import com.klu.mail.UserSignUp;
import com.klu.security.OtpSecurityService.Purpose;

class OtpSecurityServiceTests {
    private final OtpSecurityService service = new OtpSecurityService();

    @Test
    void generatesSixDigitCodeAndEnforcesPurpose() {
        UserSignUp user = new UserSignUp();
        int code = service.issue(user, Purpose.STUDENT_SIGNUP);
        assertTrue(code >= 100000 && code <= 999999);
        assertEquals("OTP Expired", service.check(user, code, Purpose.PASSWORD_RESET));
        assertNull(service.check(user, code, Purpose.STUDENT_SIGNUP));
    }

    @Test
    void blocksAfterFiveFailuresAndInvalidatesCode() {
        UserSignUp user = new UserSignUp();
        int code = service.issue(user, Purpose.REVIEWER_SIGNUP);
        for (int attempt = 1; attempt < 5; attempt++) {
            assertEquals("Invalid OTP", service.check(user, code == 100000 ? 100001 : code - 1,
                    Purpose.REVIEWER_SIGNUP));
        }
        assertTrue(service.check(user, 0, Purpose.REVIEWER_SIGNUP).startsWith("Too many"));
        assertEquals(0, user.getOtp());
        assertNotNull(service.check(user, code, Purpose.REVIEWER_SIGNUP));
    }

    @Test
    void refusesExpiredCodeAndExpiredResetGrant() {
        UserSignUp user = new UserSignUp();
        int code = service.issue(user, Purpose.PASSWORD_RESET);
        user.setOtpTimeOut(LocalDateTime.now().minusMinutes(4));
        assertEquals("OTP Expired", service.check(user, code, Purpose.PASSWORD_RESET));
        service.issue(user, Purpose.PASSWORD_RESET);
        service.verifiedForPasswordReset(user);
        assertTrue(service.canResetPassword(user));
        assertNotNull(service.check(user, code, Purpose.PASSWORD_RESET));
        user.setOtpTimeOut(LocalDateTime.now().minusMinutes(4));
        assertFalse(service.canResetPassword(user));
    }
}
