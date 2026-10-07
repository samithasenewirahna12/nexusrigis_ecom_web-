package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.ReturnRequestCreateRequest;
import com.sliit.ecommerce.dto.ReturnRequestDTO;
import com.sliit.ecommerce.service.ReturnRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/returns")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReturnRequestDTO> create(@Valid @RequestBody ReturnRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnRequestService.createReturnRequest(request));
    }


    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<ReturnRequestDTO> createMultipart(
            @RequestParam("orderId") String orderId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "refundMethod", required = false) String refundMethod,
            @RequestParam(value = "refundAmount", required = false) BigDecimal refundAmount,
            @RequestParam(value = "evidenceImage", required = false) MultipartFile evidenceImage) {

        String storedImagePath = null;
        if (evidenceImage != null && !evidenceImage.isEmpty()) {
            try {
                Path uploadPath = Paths.get("uploads", "returns").toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);

                String originalFilename = evidenceImage.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
                }
                String storedFilename = UUID.randomUUID() + extension;
                evidenceImage.transferTo(uploadPath.resolve(storedFilename));
                storedImagePath = "/uploads/returns/" + storedFilename;
            } catch (IOException ignored) {
            }
        }

        ReturnRequestCreateRequest request = new ReturnRequestCreateRequest(orderId, reason);
        request.setDescription(description);
        request.setRefundMethod(refundMethod);
        request.setRefundAmount(refundAmount);
        request.setEvidenceImage(storedImagePath);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnRequestService.createReturnRequest(request));
    }

    @GetMapping("/{id}")
    public ReturnRequestDTO get(@PathVariable String id) {
        return returnRequestService.getReturnRequest(id);
    }

    @GetMapping
    public List<ReturnRequestDTO> getAll(@RequestParam(required = false) String orderId,
                                          @RequestParam(required = false) String supportStaffId,
                                          @RequestParam(required = false) String customerId) {
        if (customerId != null) {
            return returnRequestService.getReturnsByCustomer(customerId);
        }
        if (orderId != null) {
            return returnRequestService.getReturnsByOrder(orderId);
        }
        if (supportStaffId != null) {
            return returnRequestService.getReturnsHandledBy(supportStaffId);
        }
        return returnRequestService.getAllReturns();
    }

    @RequestMapping(value = "/{id}/handle", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ReturnRequestDTO handle(@PathVariable String id,
                                    @RequestParam String supportStaffId,
                                    @RequestParam String status,
                                    @RequestParam(required = false) BigDecimal refundAmount) {
        return returnRequestService.handleReturn(id, supportStaffId, status, refundAmount);
    }
}
