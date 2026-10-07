package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.ChangePasswordRequest;
import com.sliit.ecommerce.dto.StatusUpdateRequest;
import com.sliit.ecommerce.dto.SystemUserCreateRequest;
import com.sliit.ecommerce.dto.SystemUserDTO;
import com.sliit.ecommerce.dto.SystemUserUpdateRequest;
import com.sliit.ecommerce.service.SystemUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/admin/system-users")
public class SystemUserController {

    private final SystemUserService service;

    public SystemUserController(SystemUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<SystemUserDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SystemUserDTO get(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<SystemUserDTO> create(@Valid @RequestBody SystemUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public SystemUserDTO update(@PathVariable String id,
                                @Valid @RequestBody SystemUserUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public SystemUserDTO updateStatus(@PathVariable String id, @Valid @RequestBody StatusUpdateRequest request) {
        return service.setEnabled(id, request.enabled());
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable String id,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}