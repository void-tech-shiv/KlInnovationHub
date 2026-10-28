package com.klu.mail;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klu.dto.LoginRequestDto;
import com.klu.model.Student;
import com.klu.repository.StudentRepo;
import com.klu.security.JwtService;
import com.klu.security.OtpSecurityService;
import com.klu.security.OtpSecurityService.Purpose;
import com.klu.security.ratelimit.LoginRateLimiterService;
import com.klu.security.ratelimit.PasswordResetRateLimiterService;
import com.klu.service.implementation.StudentImple;

@Service
public class MailService {

    private final AuthenticationManager authenticationManager;
	
	@Autowired
	private JavaMailSender sender;
	
	@Autowired
	private UserSignUpRepository repo;
	
	@Autowired
	private StudentImple studentService;
	
	@Autowired
	private StudentRepo studentRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private LoginRateLimiterService loginRateLimiterService;

	@Autowired
	private PasswordResetRateLimiterService passwordResetRateLimiterService;
	
	@Autowired JwtService jwtService;
	
	@Autowired private OtpSecurityService otpSecurity;
	private static final String PASSWORD_REGEX ="^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,64}$";
	private static final java.util.Set<String> STUDENT_DOMAINS = java.util.Set.of(
			"AI/ML", "Data Science", "Web Development", "Mobile App Development", "Cloud Computing",
			"Cybersecurity", "Internet of Things (IoT)", "Robotics", "Embedded Systems", "Blockchain",
			"Computer Vision", "Natural Language Processing (NLP)", "DevOps", "AR/VR", "Other");

    MailService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
	
	@Transactional
	public String generateOtp(String name,String toMail,String password,String interestedDomain) {
		if (toMail == null || !toMail.matches("\\d{10}@kluniversity\\.in")){
		    return "Use Kl University email";
		}
		Optional<UserSignUp> existingUser = repo.findLockedByMail(toMail);

        if (existingUser.isPresent()
                && !"ROLE_STUDENT".equals(existingUser.get().getRole())) {
            return "User already exists. Please login.";
        }
        if (existingUser.isPresent() && existingUser.get().isVerified()) {
            return "User already exists. Please login.";
        }
		if(password == null || !password.matches(PASSWORD_REGEX)) {
		    return "Password does not meet security requirements";
		}
		if (interestedDomain == null || !STUDENT_DOMAINS.contains(interestedDomain)) {
		    return "Select a valid interested domain";
		}
        if (!passwordResetRateLimiterService.tryRequest(toMail).isAllowed()) {
            return "Too many OTP requests. Please try again later.";
        }
        UserSignUp user = existingUser.orElseGet(UserSignUp::new);
        user.setName(name);
        user.setMail(toMail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_STUDENT");
        user.setInterestedDomain(interestedDomain);
        user.setVerified(false);
        int otpnum = otpSecurity.issue(user, Purpose.STUDENT_SIGNUP);
        repo.save(user);
        SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(toMail);
		msg.setSubject("Verify Your Email • KL Innovation Hub");
		msg.setText(
			"Hello " + name + ",\n\n" +

			"Welcome to KL Innovation Hub!\n\n" +

			"Use the OTP below to verify your email address:\n\n" +

			"━━━━━━━━━━━━━━━━━━━━━━\n" +
			"        OTP : " + otpnum + "\n" +
			"━━━━━━━━━━━━━━━━━━━━━━\n\n" +

			"This OTP is valid for 3 minutes.\n\n" +

			"For your security:\n" +
			"• Never share this OTP with anyone.\n" +

			"If you didn't request this verification, you can safely ignore this email.\n\n" +

			"Happy Building!\n\n" +

			"—\n" +
			"KL Innovation Hub\n" +
			"Build • Collaborate • Innovate"
		);
		sender.send(msg);
		return "If the email exists, OTP has been sent";
	}

    @Transactional
    public String verifyOtp(String mail, int recOtp) {
        UserSignUp account = repo.findLockedByMail(mail).orElse(null);
        if (account == null || !"ROLE_STUDENT".equals(account.getRole())) {
            return "OTP Expired";
        }
        if (account.isVerified()) {
            return "User already verified";
        }
        String error = otpSecurity.check(account, recOtp, Purpose.STUDENT_SIGNUP);
        if (error != null) {
            repo.save(account);
            return error;
        }
        try {
            studentService.CreateStudentByEmail(account.getMail(), account.getName(),
                    account.getInterestedDomain());
            account.setVerified(true);
            otpSecurity.invalidate(account);
            repo.save(account);
            return "Verified You Can SignIn Now";
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

	public LoginRequestDto login(Login req) {
		
		UserSignUp recMail = repo.findByMail(req.getMail()).orElse(null);
		if (loginRateLimiterService.isBlocked(req.getMail())) {
		    long minutes =loginRateLimiterService.getRemainingLockMinutes(req.getMail());
		    return new LoginRequestDto("Too many failed login attempts. Try again in " + minutes + " minute(s).", null,null, null,null);
		}
		
		if(recMail==null) {
			return new LoginRequestDto("Mail Not Found",null,null,null,null);
		}
		
		if (!recMail.getMail().endsWith("@kluniversity.in")) {
		    return new LoginRequestDto("Use valid university email",null,null,null,null);
		}
		
		if (!recMail.isVerified()) {
	        return new LoginRequestDto("Please verify email first",null,null,null,null);
	    }
		
		String token=null;
		try {
			Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getMail(),req.getPassword()));
			token = jwtService.generateToken(auth.getName());
			loginRateLimiterService.loginSucceeded(req.getMail());
		}catch (BadCredentialsException e) {
			loginRateLimiterService.loginFailed(req.getMail());
			return new LoginRequestDto("Bad Credentials",null,null,null,null);
		}
		
		
		Student s = studentRepo.findByStudentEmail(recMail.getMail());
		if(s == null){
			return new LoginRequestDto("Student profile not found",null,null,null,null);
		}
		return new LoginRequestDto("Welcome To DashBoard",s,s.getStudentId(),s.getStudentEmail(),token);
	}

    @Transactional
    public String reSend(String mail) {
        if (mail == null) return "Mail Not Found";
        UserSignUp account = repo.findLockedByMail(mail).orElse(null);
        if (account == null) return "Mail Not Found";
        if (!"ROLE_STUDENT".equals(account.getRole())) return "Invalid account";
        if (account.isVerified()) return "User already verified";

        LocalDateTime issuedAt = account.getOtpTimeOut();
        if (issuedAt != null && LocalDateTime.now().isBefore(issuedAt.plusMinutes(3))) {
            return "Wait 3 minutes before resending a verification email";
        }
        if (!passwordResetRateLimiterService.tryRequest(mail).isAllowed()) {
            return "Too many OTP requests. Please try again later.";
        }
        int otpnum = otpSecurity.issue(account, Purpose.STUDENT_SIGNUP);
        repo.save(account);
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(mail);
		msg.setSubject("Your New Verification Code • KL Innovation Hub");
		msg.setText(
			"Hello,\n\n" +

			"Here is your new verification code.\n\n" +

			"━━━━━━━━━━━━━━━━━━━━━━\n" +
			"        OTP : " + otpnum + "\n" +
			"━━━━━━━━━━━━━━━━━━━━━━\n\n" +

			"This OTP is valid for 3 minutes.\n\n" +

			"Your previous OTP is no longer valid.\n\n" +

			"If you didn't request this email, you can ignore it.\n\n" +

			"—\n" +
			"KL Innovation Hub"
		);
	
        sender.send(msg);
        return "Check your inbox for otp";
    }

	@Transactional
	public String forgotPassword(String mail) {

	    UserSignUp user = mail == null ? null : repo.findLockedByMail(mail).orElse(null);

	    if(user == null || !"ROLE_STUDENT".equals(user.getRole())) {
	        return "Mail Not Found";
	    }
	    if(!user.isVerified()) {
	        return "Please verify account first";
	    }

	    PasswordResetRateLimiterService.RateLimitResult rateLimit =
	            passwordResetRateLimiterService.tryRequest(mail);
	    if (!rateLimit.isAllowed()) {
	        return "Too many password reset requests. Please try again later.";
	    }

	    int otp = otpSecurity.issue(user, Purpose.PASSWORD_RESET);
        repo.save(user);

	    SimpleMailMessage msg = new SimpleMailMessage();
	    msg.setTo(mail);
		msg.setSubject("Reset Your Password • KL Innovation Hub");

		msg.setText(
			"Hello,\n\n" +

			"We received a request to reset your password.\n\n" +

			"Use the OTP below to continue:\n\n" +

			"━━━━━━━━━━━━━━━━━━━━━━\n" +
			"        OTP : " + otp + "\n" +
			"━━━━━━━━━━━━━━━━━━━━━━\n\n" +

			"This OTP expires in 3 minutes.\n\n" +

			"If you didn't request a password reset, please ignore this email.\n\n" +

			"For your security, never share this OTP.\n\n" +

			"—\n" +
			"KL Innovation Hub\n" +
			"Build • Collaborate • Innovate"
		);

	    sender.send(msg);

	    return "OTP Sent";
	}
	
    @Transactional
    public String verifyResetOtp(String mail, int otp) {
        UserSignUp user = mail == null ? null : repo.findLockedByMail(mail).orElse(null);
        if (user == null || !"ROLE_STUDENT".equals(user.getRole()) || !user.isVerified()) {
            return "OTP Expired";
        }
        String error = otpSecurity.check(user, otp, Purpose.PASSWORD_RESET);
        if (error != null) {
            repo.save(user);
            return error;
        }
        otpSecurity.verifiedForPasswordReset(user);
        repo.save(user);
        return "OTP Verified";
    }

    @Transactional
    public String resetPassword(String mail, String newPassword) {
        UserSignUp user = mail == null ? null : repo.findLockedByMail(mail).orElse(null);
        if (user == null || !"ROLE_STUDENT".equals(user.getRole()) || !user.isVerified()) {
            return "Mail Not Found";
        }
        if (newPassword == null || !newPassword.matches(PASSWORD_REGEX)) {
            return "Password does not meet security requirements";
        }
        if (!otpSecurity.canResetPassword(user)) {
            return "Verify OTP First";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        otpSecurity.invalidate(user);
        repo.save(user);
        return "Password Updated Successfully";
    }
}
