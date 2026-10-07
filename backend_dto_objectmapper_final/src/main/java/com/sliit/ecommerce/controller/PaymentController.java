package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.PaymentCreateRequest;
import com.sliit.ecommerce.dto.PaymentDTO;
import com.sliit.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentDTO> create(@Valid @RequestBody PaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    @GetMapping("/{id}")
    public PaymentDTO get(@PathVariable String id) {
        return paymentService.getPayment(id);
    }

    @GetMapping
    public PaymentDTO getByOrder(@RequestParam String orderId) {
        return paymentService.getPaymentByOrder(orderId);
    }
}
