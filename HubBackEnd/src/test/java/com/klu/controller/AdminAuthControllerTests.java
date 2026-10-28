package com.klu.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.klu.dto.AdminLoginResponseDto;
import com.klu.mail.Login;
import com.klu.service.implementation.AdminLoginService;

class AdminAuthControllerTests {

    private final AdminLoginService loginService = mock(AdminLoginService.class);
    private AdminAuthController controller;

    @BeforeEach
    void setup() {
        controller = new AdminAuthController();
        ReflectionTestUtils.setField(controller, "adminLoginService", loginService);
        reset(loginService);
    }

    @Test
    void invalidCredentialsReturn401WithoutToken() {
        Login request = new Login("admin@example.com", "wrong");
        when(loginService.login(request)).thenReturn(
                new AdminLoginResponseDto("Invalid admin credentials", null, null, null));

        var result = controller.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(result.getBody().getToken());
    }

    @Test
    void rateLimitReturns429AndRetryAfterHeader() {
        Login request = new Login("admin@example.com", "wrong");
        when(loginService.login(request)).thenReturn(
                new AdminLoginResponseDto(
                        "Too many login attempts. Please try again later.", null, null, null));
        when(loginService.retryAfterSeconds("admin@example.com")).thenReturn(240L);

        var result = controller.login(request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, result.getStatusCode());
        assertEquals("240", result.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertNull(result.getBody().getToken());
    }

    @Test
    void successReturns200AndAdminToken() {
        Login request = new Login("admin@example.com", "valid");
        when(loginService.login(request)).thenReturn(
                new AdminLoginResponseDto(
                        "Welcome To Admin Dashboard", "Admin", "admin@example.com", "jwt"));

        var result = controller.login(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("jwt", result.getBody().getToken());
    }
}
