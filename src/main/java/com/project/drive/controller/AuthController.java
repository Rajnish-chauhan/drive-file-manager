package com.project.drive.controller;

import com.project.drive.entity.UserEntity;
import com.project.drive.repo.UserRepository;
import com.project.drive.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // 6-digit secure OTP generator
    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(100000, 999999));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserEntity user) {
        // 1. Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists!"));
        }

        // 2. Prepare new user with unverified status and OTP
        String otp = generateOTP();
        user.setOtp(otp);
        user.setVerified(false); // User cannot login yet
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. Save user to DB to store the OTP
        userRepository.save(user);

        // 4. Try sending the OTP email
        try {
            emailService.sendVerificationEmail(user.getEmail(), otp);
        } catch (Exception e) {
            // SECURITY FIX: If email fails, delete the unverified user so they can try again later!
            userRepository.delete(user);
            System.out.println("Email Error: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Error sending verification email. Please check your Mail App Password."));
        }

        return ResponseEntity.ok(Map.of("message", "Registration successful! Please verify OTP via your email."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found!"));
        }

        UserEntity user = userOpt.get();
        if (user.isVerified()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User is already verified!"));
        }

        // STRICT OTP MATCH: Must exactly match the DB value
        if (user.getOtp() != null && user.getOtp().equals(otp)) {
            user.setVerified(true); // Approve login
            user.setOtp(null);      // Destroy OTP after successful use
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully! You can now log in."));
        }

        // If OTP is wrong, block them
        return ResponseEntity.status(401).body(Map.of("message", "Invalid OTP!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity loginData, HttpServletRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(loginData.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User does not exist. Please sign up first!"));
        }

        UserEntity user = userOpt.get();

        //Guard against unverified logins
        if (!user.isVerified()) {
            return ResponseEntity.status(403).body(Map.of("message", "Please verify your email before logging in!"));
        }

        if (!passwordEncoder.matches(loginData.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect password!"));
        }

        // Establish session
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.emptyList());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return ResponseEntity.ok(Map.of("message", "Login Successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal().toString())) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthenticated execution status."));
        }

        Map<String, Object> userInfo = new HashMap<>();

        if (auth.getPrincipal() instanceof OAuth2User oauthUser) {
            String name = oauthUser.getAttribute("name") != null ? oauthUser.getAttribute("name") : oauthUser.getAttribute("login");
            String avatar = oauthUser.getAttribute("picture") != null ? oauthUser.getAttribute("picture") : oauthUser.getAttribute("avatar_url");
            userInfo.put("name", name);
            userInfo.put("email", oauthUser.getAttribute("email"));
            userInfo.put("avatar", avatar);
        } else {
            String email = (String) auth.getPrincipal();
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                userInfo.put("name", userOpt.get().getName());
                userInfo.put("email", userOpt.get().getEmail());
                userInfo.put("avatar", "https://cdn-icons-png.flaticon.com/512/149/149071.png");
            }
        }

        return ResponseEntity.ok(userInfo);
    }
}