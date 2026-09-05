package com.project.drive.controller;

import com.project.drive.dto.LoginRequest;
import com.project.drive.dto.RegisterRequest;
import com.project.drive.dto.VerifyOtpRequest;
import com.project.drive.entity.UserEntity;
import com.project.drive.repo.UserRepository;
import com.project.drive.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository; // Sirf /me endpoint ke liye chahiye

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful! Please verify OTP via your email."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully! You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        authService.loginUser(request, httpRequest);
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