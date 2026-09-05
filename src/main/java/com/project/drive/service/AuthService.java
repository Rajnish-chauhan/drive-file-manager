package com.project.drive.service;

import com.project.drive.dto.LoginRequest;
import com.project.drive.dto.RegisterRequest;
import com.project.drive.dto.VerifyOtpRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    void registerUser(RegisterRequest request);
    void verifyOtp(VerifyOtpRequest request);
    void loginUser(LoginRequest request, HttpServletRequest httpRequest);
}