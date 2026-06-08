package com.project.drive.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.password}")
    private String fetchedPassword;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }



    public void sendVerificationEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your Secure Drive App Verification Code");
        message.setText("Welcome to Cloud Drive!\n\n" +
                "Your verification OTP code is: " + otp + "\n\n" +
                "This code expires shortly. Please do not share it with anyone.");
        mailSender.send(message);
    }
}