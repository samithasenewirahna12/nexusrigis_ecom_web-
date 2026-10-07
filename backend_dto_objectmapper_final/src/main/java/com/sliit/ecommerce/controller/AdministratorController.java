package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.AdministratorCreateRequest;
import com.sliit.ecommerce.dto.AdministratorDTO;
import com.sliit.ecommerce.dto.AdministratorUpdateRequest;
import com.sliit.ecommerce.dto.ChangePasswordRequest;
import com.sliit.ecommerce.service.AdministratorProfileService;
import com.sliit.ecommerce.service.AdministratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/administrators")
public class AdministratorController {

    private final AdministratorService administratorService;
    private final AdministratorProfileService profileService;

    public AdministratorController(AdministratorService administratorService, AdministratorProfileService profileService) {
        this.administratorService = administratorService;
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<AdministratorDTO> create(@Valid @RequestBody AdministratorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administratorService.createAdministrator(request));
    }

    @GetMapping("/{id}")
    public AdministratorDTO get(@PathVariable String id) {
        return administratorService.getAdministrator(id);
    }

    @GetMapping
    public List<AdministratorDTO> getAll() {
        return administratorService.getAllAdministrators();
    }

    /* ---- NEW: used by the Admin Profile page ---- */

    @PutMapping("/{id}")
    public AdministratorDTO updateProfile(@PathVariable String id,
                                          @Valid @RequestBody AdministratorUpdateRequest request) {
        return profileService.updateProfile(id, request);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable String id,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        administratorService.deleteAdministrator(id);
        return ResponseEntity.noContent().build();
    }
}