package com.project.drive.controller;


import com.project.drive.entity.UserEntity;
import com.project.drive.repo.UserRepository;
import com.project.drive.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
// Ye line confirm karo (port 5173 tumhare Vite ka port hai)
@CrossOrigin(origins = {"https://drive.rajnishsystems.in", "http://localhost:5173"}, allowCredentials = "true")public class AuthController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    public AuthController(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }


    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists!"));
        }

        // Generate OTP and set user to unverified
        String otp = generateOTP();
        user.setOtp(otp);
        user.setVerified(false);
        userRepository.save(user);

        // Send the email
        try {
            emailService.sendVerificationEmail(user.getEmail(), otp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error sending email. Is it a real email?"));
        }

        return ResponseEntity.ok(Map.of("message", "Registration successful! Please check your email for the OTP."));
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

        if (user.getOtp().equals(otp)) {
            user.setVerified(true);
            user.setOtp(null); // Clear OTP after success
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully! You can now log in."));
        }

        return ResponseEntity.status(401).body(Map.of("message", "Invalid OTP!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity loginData, HttpServletRequest request, HttpServletResponse response) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(loginData.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User does not exist. Please sign up first!"));
        }

        UserEntity user = userOpt.get();

        // NAYA: Block login if email is not verified
        if (!user.isVerified()) {
            return ResponseEntity.status(403).body(Map.of("message", "Please verify your email before logging in!"));
        }

        if (!user.getPassword().equals(loginData.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect password!"));
        }

        // ... [Rest of your existing session login code here] ...
        return ResponseEntity.ok(Map.of("message", "Login Successful"));
    }
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(null);
        }

        Map<String, Object> userInfo = new HashMap<>();

        if (auth.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
            userInfo.put("name", oauthUser.getAttribute("name") != null ? oauthUser.getAttribute("name") : oauthUser.getAttribute("login"));
            userInfo.put("email", oauthUser.getAttribute("email"));
            userInfo.put("avatar", oauthUser.getAttribute("picture") != null ? oauthUser.getAttribute("picture") : oauthUser.getAttribute("avatar_url"));
        } else {
            String email = (String) auth.getPrincipal();
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if(userOpt.isPresent()) {
                userInfo.put("name", userOpt.get().getName());
                userInfo.put("email", userOpt.get().getEmail());
                userInfo.put("avatar", "https://cdn-icons-png.flaticon.com/512/149/149071.png");
            }
        }

        return ResponseEntity.ok(userInfo);
    }
}