package com.project.drive.service;

import com.project.drive.dto.LoginRequest;
import com.project.drive.dto.RegisterRequest;
import com.project.drive.dto.VerifyOtpRequest;
import com.project.drive.entity.UserEntity;
import com.project.drive.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(100000, 999999));
    }

    @Override
    public void registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists!");
        }

        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        String otp = generateOTP();
        user.setOtp(otp);
        user.setVerified(false);

        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), otp);
        } catch (Exception e) {
            userRepository.delete(user);
            throw new RuntimeException("Error sending verification email. Please check your Mail App Password.");
        }
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (user.isVerified()) {
            throw new RuntimeException("User is already verified!");
        }

        if (user.getOtp() != null && user.getOtp().equals(request.getOtp())) {
            user.setVerified(true);
            user.setOtp(null);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Invalid OTP!");
        }
    }

    @Override
    public void loginUser(LoginRequest request, HttpServletRequest httpRequest) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User does not exist. Please sign up first!"));

        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your email before logging in!");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password!");
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.emptyList());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}