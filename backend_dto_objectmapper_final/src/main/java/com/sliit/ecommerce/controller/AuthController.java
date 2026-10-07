package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.AuthResponse;
import com.sliit.ecommerce.dto.LoginRequest;
import com.sliit.ecommerce.dto.RegisterRequest;
import com.sliit.ecommerce.dto.StaffRegisterRequest;
import com.sliit.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Errors (ApiException, validation) are turned into { "message": "..." }
 * by GlobalExceptionHandler, so no try/catch is needed here.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/staff-register")
    public ResponseEntity<AuthResponse> staffRegister(@Valid @RequestBody StaffRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.staffRegister(request));
    }

    @PostMapping("/staff-login")
    public ResponseEntity<AuthResponse> staffLogin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.staffLogin(request));
    }
}