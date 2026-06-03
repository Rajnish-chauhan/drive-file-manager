package com.project.drive.controller;


import com.project.drive.entity.UserEntity;
import com.project.drive.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
// Ye line confirm karo (port 5173 tumhare Vite ka port hai)
@CrossOrigin(origins = "https://drive.rajnishsystems.in", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 1. Custom Register API
    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {

            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists!"));
        }
        userRepository.save(user);
        // Success message in JSON format
        return ResponseEntity.ok(Map.of("message", "Account created successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity loginData, HttpServletRequest request, HttpServletResponse response) {
        System.out.println("--- LOGIN ATTEMPT ---");
        System.out.println("Email Entered: " + loginData.getEmail());
        System.out.println("Password Entered: " + loginData.getPassword());

        // 1. Database  email search
        Optional<UserEntity> userOpt = userRepository.findByEmail(loginData.getEmail());

        // 2. if email DB not found
        if (userOpt.isEmpty()) {
            System.out.println("RESULT: User not found in DB!");
            return ResponseEntity.status(404).body(Map.of("message", "User does not exist. Please sign up first!"));
        }

        // 3. if email fount but, Password wrong
        if (!userOpt.get().getPassword().equals(loginData.getPassword())) {
            System.out.println("RESULT: Password did not match!");
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect password!"));
        }

        // 4. If both match (Success)
        System.out.println("RESULT: Login 100% Successful!");
        UserEntity user = userOpt.get();

        // Session logic
        UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.emptyList());
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(authReq);
        SecurityContextHolder.setContext(sc);

        HttpSessionSecurityContextRepository securityContextRepo = new HttpSessionSecurityContextRepository();
        securityContextRepo.saveContext(sc, request, response);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail());
        userInfo.put("message", "Login Successful"); // Success message bhej rahe hain

        return ResponseEntity.ok(userInfo);
    }
    // 3. Get Current User
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