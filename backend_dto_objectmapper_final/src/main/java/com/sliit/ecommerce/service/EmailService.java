package com.sliit.ecommerce.service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInvoiceEmail(String customerEmail, String orderId, MultipartFile file) {
        try {
            final byte[] fileBytes = file.getBytes();
            final String originalFileName = file.getOriginalFilename();

            // Run mail dispatch asynchronously so the client HTTP request returns in milliseconds
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                    helper.setFrom(fromEmail);
                    helper.setTo(customerEmail);
                    helper.setSubject("NexusRigs Invoice - Order #" + orderId);

                    String body = "Hello,\n\n" +
                            "Thank you for shopping with NexusRigs.\n\n" +
                            "Your official invoice for order #" + orderId + " is attached to this email.\n\n" +
                            "Order Reference: #" + orderId + "\n" +
                            "Warranty: 2-Year Official NexusRigs Hardware Guarantee\n\n" +
                            "Thank you for choosing NexusRigs.\n\n" +
                            "NexusRigs Store\n" +
                            "www.nexusrigs.com\n" +
                            "support@nexusrigs.com";

                    helper.setText(body, false);

                    String fileName = originalFileName;
                    if (fileName == null || fileName.trim().isEmpty()) {
                        fileName = "NexusRigs-Invoice-" + orderId + ".pdf";
                    }

                    helper.addAttachment(fileName, new org.springframework.core.io.ByteArrayResource(fileBytes));

                    System.out.println("Async dispatching invoice email to: " + customerEmail);
                    mailSender.send(message);
                    System.out.println("Invoice email dispatched successfully to: " + customerEmail);

                } catch (Exception ex) {
                    System.err.println("Background invoice email sending error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            System.err.println("Failed to read invoice file for dispatch: " + e.getMessage());
            throw new RuntimeException("Failed to prepare invoice email: " + e.getMessage(), e);
        }
    }
}