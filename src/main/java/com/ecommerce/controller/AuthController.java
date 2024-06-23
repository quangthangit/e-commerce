package com.ecommerce.controller;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.Service.JwtService;
import com.ecommerce.Service.EmailService;
import com.ecommerce.Service.UserService;
import com.ecommerce.dto.MessageResponse;
import com.ecommerce.dto.UserRequest;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.model.Email;
import com.ecommerce.model.User;
import com.ecommerce.model.VerificationToken;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.VerificationTokenRepository;
import com.ecommerce.util.EmailUtil;

@RestController
public class AuthController {

    private static final String USER_ROLE = "USER";
    private static final String EMAIL_VERIFICATION_SUBJECT = "Click the link below to verify your email address";
    private static final String EMAIL_VERIFICATION_BODY_TEMPLATE = "<div style=\"background-color: #f3f3f3; padding: 20px; font-family: Arial, sans-serif; text-align: center;\">" +
            "<h3 style=\"color: #333;\">Click the link below to verify your email address:</h3>" +
            "<a href=\"%s\" style=\"color:#119744; font-weight: bold;\">Verify Email</a></div>";
    private static final long TOKEN_EXPIRATION_TIME = 24 * 60 * 60 * 1000; 

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private EmailService mailService;
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Value("${mail.url}")
    private String baseUrl;
    
    @PostMapping("/authenticate/register")
    public ResponseEntity<?> createUser(@RequestBody @Validated User user, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
        	return ResponseEntity.status(HttpStatus.FOUND).body(new MessageResponse("Email already registered"));
        }

        user.setRole(USER_ROLE);
        user.setEnabled(false);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        createVerificationToken(user.getEmail(), token);
        sendVerificationEmail(user.getEmail(), token);

        return ResponseEntity.status(HttpStatus.OK).body(new MessageResponse("User registered successfully. Please check your email to verify your account."));
    }

    @PostMapping("/authenticate/login")
    public ResponseEntity<?> authenticateAndGetToken(@RequestBody UserRequest loginReq) {
        Authentication authentication = authenticateUser(loginReq);
        if (!authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid credentials"));
        }

        User user = findUserByEmail(loginReq.getUsername());
        if (!user.isEnabled()) {
        	return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("Account is not activated"));
        }

        String accessToken = generateTokenForUser(loginReq.getUsername());
        UserResponse userResponse = buildUserResponse(user, accessToken);

        return ResponseEntity.ok(userResponse);
    }
    
    

    @GetMapping("/user/home")
    public ResponseEntity<?> user() {
    	return ResponseEntity.ok(new MessageResponse("USER LOGIN"));
    } 
    
    @GetMapping("/admin/home")
    public ResponseEntity<?> admin() {
        return ResponseEntity.ok(new MessageResponse("ADMIN LOGIN"));
    } 
    
    private Authentication authenticateUser(UserRequest loginReq) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginReq.getUsername(), loginReq.getPassword()));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String generateTokenForUser(String username) {
        return jwtService.generateToken(userService.loadUserByUsername(username));
    }

    private UserResponse buildUserResponse(User user, String accessToken) {
		UserResponse userResponse = new UserResponse();
		userResponse.setAccess_token(accessToken);
		userResponse.setToken_type("Bearer ");
		userResponse.setName(user.getName());
		userResponse.setExpires_in(TimeUnit.MINUTES.toMillis(30));
		userResponse.setAvatar(user.getAvatar());
		userResponse.setPhone(user.getPhone());
		userResponse.setRole(user.getRole());
		userResponse.setId(user.getUserId());
		userResponse.setAddress(user.getAddress());
		userResponse.setEmail(user.getEmail());
		return userResponse;
	}

    private void createVerificationToken(String email, String token) {
        VerificationToken verificationToken = new VerificationToken(token, email, new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_TIME));
        verificationTokenRepository.save(verificationToken);
    }

    private void sendVerificationEmail(String email, String token) {
        String confirmationUrl = baseUrl + "/mail/confirm?token=" + token;
        String body = String.format(EMAIL_VERIFICATION_BODY_TEMPLATE, confirmationUrl);
        try {
			EmailUtil.sendVerificationEmail(email, token, mailService, EMAIL_VERIFICATION_SUBJECT, body);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private ResponseEntity<MessageResponse> buildForbiddenResponse(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(message));
    }
}
