package com.edtech.lms.notification.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * EmailService - Handles the processing and dispatching of transactional emails.
 * 
 * Uses Spring's JavaMailSender to send formatted emails to users for 
 * onboarding, password resets, etc.
 */
@Service
@lombok.RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("classpath:templates/email/onboarding.txt")
    private Resource onboardingTemplateResource;

    @Value("classpath:templates/email/password_reset.txt")
    private Resource passwordResetTemplateResource;

    @Value("classpath:templates/email/premium_upgrade.txt")
    private Resource premiumUpgradeTemplateResource;

    private String onboardingTemplate;
    private String passwordResetTemplate;
    private String premiumUpgradeTemplate;

    /**
     * Initializes the service by loading email templates from the classpath into memory.
     */
    @PostConstruct
    public void init() {
        try {
            onboardingTemplate = StreamUtils.copyToString(onboardingTemplateResource.getInputStream(), StandardCharsets.UTF_8);
            passwordResetTemplate = StreamUtils.copyToString(passwordResetTemplateResource.getInputStream(), StandardCharsets.UTF_8);
            premiumUpgradeTemplate = StreamUtils.copyToString(premiumUpgradeTemplateResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to load email templates during application startup", e);
        }
    }

    /**
     * Sends a confirmation email to the company admin after a successful premium upgrade.
     * 
     * @param toEmail The admin's email address
     * @param username The admin's name
     */
    public void sendPremiumUpgradeConfirmation(String toEmail, String username) {
        String subject = "Welcome to Enterprise LMS Premium!";
        String body = premiumUpgradeTemplate
                .replace("{{username}}", username != null ? username : "");
        
        sendEmail(toEmail, subject, body);
    }

    /**
     * Sends the initial onboarding credentials email containing the temporary password.
     * 
     * @param toEmail The recipient's email address
     * @param username The username assigned to the user
     * @param temporaryPassword The generated temporary password
     * @param roleName The role of the user (e.g. EMPLOYEE, COMPANY_ADMIN)
     * @throws IllegalArgumentException if the email is invalid
     * @throws MailException if the email fails to send
     */
    public void sendOnboardingCredentials(String toEmail, String username, String temporaryPassword, String roleName) {
        String subject = "Welcome to Enterprise LMS - Your " + roleName + " Credentials";
        String body = onboardingTemplate
                .replace("{{username}}", username != null ? username : "")
                .replace("{{roleName}}", roleName != null ? roleName : "")
                .replace("{{email}}", toEmail != null ? toEmail : "")
                .replace("{{password}}", temporaryPassword != null ? temporaryPassword : "");
        
        sendEmail(toEmail, subject, body);
    }

    /**
     * Sends a password reset verification code.
     * 
     * @param toEmail The recipient's email address
     * @param username The username requesting the reset
     * @param verificationCode The generated 6-digit code
     * @throws IllegalArgumentException if the email is invalid
     * @throws MailException if the email fails to send
     */
    public void sendPasswordResetCode(String toEmail, String username, String verificationCode) {
        String subject = "Enterprise LMS - Password Reset Verification Code";
        String body = passwordResetTemplate
                .replace("{{username}}", username != null ? username : "")
                .replace("{{verificationCode}}", verificationCode != null ? verificationCode : "");
        
        sendEmail(toEmail, subject, body);
    }

    /**
     * Internal utility to format and dispatch the email using JavaMailSender.
     * 
     * @param to The recipient's email address
     * @param subject The email subject
     * @param body The plaintext body of the email
     * @throws IllegalArgumentException if the email format is invalid
     * @throws MailException if the SMTP server rejects the email
     */
    public void sendEmail(String to, String subject, String body) {
        if (to == null || !to.contains("@")) {
            throw new IllegalArgumentException("Invalid email address provided: " + to);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@enterpriselms.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Email successfully dispatched to {}", to);
        } catch (MailException e) {
            logger.error("Failed to dispatch email to {}. Root cause: {}", to, e.getMessage());
            throw e; 
        }
    }
}
