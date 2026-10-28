package com.klu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.AdminLoginResponseDto;
import com.klu.mail.Login;
import com.klu.service.implementation.AdminLoginService;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @Autowired
    private AdminLoginService adminLoginService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponseDto> login(@RequestBody Login request) {
        AdminLoginResponseDto response = adminLoginService.login(request);
        if (response.getMessage().startsWith("Too many login attempts")) {
            long seconds = Math.max(1L, adminLoginService.retryAfterSeconds(
                    request == null ? null : request.getMail()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                    .body(response);
        }
        if (response.getToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
