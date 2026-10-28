package com.klu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.LoginRequestDto;
import com.klu.dto.ResetPasswordDto;
import com.klu.dto.SignUpFormDto;
import com.klu.dto.VerifyOtpDto;
import com.klu.dto.VerifyResetOtpDto;
import com.klu.mail.Login;
import com.klu.mail.MailService;

@RestController
@RequestMapping("/mail")
public class MailController {

	@Autowired
	MailService service;
	
	@PostMapping("/generateOtp")
	public ResponseEntity<String> generateOtp(@RequestBody SignUpFormDto form) {
        String result = service.generateOtp(form.getName(), form.getMail(),
                form.getPassword(), form.getInterestedDomain());
        return otpResponse(result);
    }
	
	@PostMapping("/verifyOtp")
	public ResponseEntity<String> verifyOtp(@RequestBody VerifyOtpDto dto) {
        return otpResponse(service.verifyOtp(dto.getMail(), dto.getOtp()));
    }
	
	@PostMapping("/login")
	public ResponseEntity<LoginRequestDto> login(@RequestBody Login req) {
	    LoginRequestDto response = service.login(req);
	    if (response.getMessage().startsWith("Too many")) {
	        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
	    }
	    return ResponseEntity.ok(response);
	}
	
	@PostMapping("/resend")
	public ResponseEntity<String> reSend(@RequestParam String mail) {
        return otpResponse(service.reSend(mail));
    }
	
	@PostMapping("/forgotPassword")
	public ResponseEntity<String> forgotPassword(@RequestParam String mail) {
	    String response = service.forgotPassword(mail);
	    if (response.startsWith("Too many password reset requests")) {
	        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
	    }
	    return ResponseEntity.ok(response);
	}
	
	@PostMapping("/verifyResetOtp")
	public ResponseEntity<String> verifyResetOtp(@RequestBody VerifyResetOtpDto dto) {
	    String response = service.verifyResetOtp(dto.getMail(), dto.getOtp());
	    if (response.startsWith("Too many invalid OTP attempts")) {
	        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
	    }
	    return ResponseEntity.ok(response);
	}
	
    @PostMapping("/resetPassword")
    public String resetPassword(@RequestBody ResetPasswordDto dto) {
        return service.resetPassword(dto.getMail(), dto.getNewPassword());
    }

    private ResponseEntity<String> otpResponse(String result) {
        if (result.startsWith("Too many")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
