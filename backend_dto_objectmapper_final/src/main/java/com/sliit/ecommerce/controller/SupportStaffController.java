package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.SupportStaffCreateRequest;
import com.sliit.ecommerce.dto.SupportStaffDTO;
import com.sliit.ecommerce.service.SupportStaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support-staff")
public class SupportStaffController {

    private final SupportStaffService supportStaffService;

    public SupportStaffController(SupportStaffService supportStaffService) {
        this.supportStaffService = supportStaffService;
    }

    @PostMapping
    public ResponseEntity<SupportStaffDTO> create(@Valid @RequestBody SupportStaffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportStaffService.createSupportStaff(request));
    }

    @GetMapping("/{id}")
    public SupportStaffDTO get(@PathVariable String id) {
        return supportStaffService.getSupportStaff(id);
    }

    @GetMapping
    public List<SupportStaffDTO> getAll() {
        return supportStaffService.getAllSupportStaff();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        supportStaffService.deleteSupportStaff(id);
        return ResponseEntity.noContent().build();
    }
}
