package com.sliit.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BillService {

    private final EmailService emailService;

    public BillService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendInvoice(String email, String orderId, MultipartFile file) {

        emailService.sendInvoiceEmail(email, orderId, file);
    }
}