package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.service.BillService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping(value = "/send-invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> sendInvoice( @RequestParam("email") String email, @RequestParam("orderId") String orderId, @RequestPart("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {

            if (email == null || email.trim().isEmpty()) {

                response.put("success", false);

                response.put("message", "Email address is required.");

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            if (orderId == null || orderId.trim().isEmpty()) {

                response.put("success", false);

                response.put("message", "Order ID is required.");

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            if (file == null || file.isEmpty()) {

                response.put("success", false);

                response.put("message", "Invoice PDF file is required.");

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            String fileName = file.getOriginalFilename();

            if (fileName == null || !fileName
                            .toLowerCase()
                            .endsWith(".pdf")) {

                response.put("success", false);

                response.put("message", "Only PDF invoice files are allowed.");

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            System.out.println("======================================");
            System.out.println("Invoice email request received");
            System.out.println("Email: " + email);
            System.out.println("Order ID: " + orderId);
            System.out.println("File: " + fileName);
            System.out.println("File size: " + file.getSize() + " bytes");
            System.out.println("======================================");

            billService.sendInvoice(
                    email.trim(),
                    orderId.trim(),
                    file
            );

            response.put("success", true);

            response.put("message", "Invoice sent successfully to " + email.trim());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            System.err.println("======================================");
            System.err.println("INVOICE EMAIL ERROR");

            e.printStackTrace();

            System.err.println("======================================");

            response.put("success", false);

            response.put("message",
                    e.getMessage() != null ? e.getMessage() : "Unable to send invoice email."
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }


    @GetMapping("/send-invoice")
    public ResponseEntity<Map<String, Object>>
    invoiceEndpointInfo() {

        Map<String, Object> response = new HashMap<>();

        response.put("success", true);

        response.put(
                "message",
                "Invoice endpoint is working. " + "Use POST with multipart/form-data to send an invoice."
        );

        return ResponseEntity.ok(response);
    }
}