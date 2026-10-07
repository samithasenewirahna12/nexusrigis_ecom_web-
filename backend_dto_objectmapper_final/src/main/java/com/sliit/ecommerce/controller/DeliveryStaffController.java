package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.DeliveryStaffCreateRequest;
import com.sliit.ecommerce.dto.DeliveryStaffDTO;
import com.sliit.ecommerce.service.DeliveryStaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-staff")
public class DeliveryStaffController {

    private final DeliveryStaffService deliveryStaffService;


    public DeliveryStaffController(DeliveryStaffService deliveryStaffService) {
        this.deliveryStaffService = deliveryStaffService;
    }

    @PostMapping
    public ResponseEntity<DeliveryStaffDTO> create(@Valid @RequestBody DeliveryStaffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryStaffService.createDeliveryStaff(request));
    }

    @GetMapping("/{id}")
    public DeliveryStaffDTO get(@PathVariable String id) {
        return deliveryStaffService.getDeliveryStaff(id);
    }

    @GetMapping
    public List<DeliveryStaffDTO> getAll() {
        return deliveryStaffService.getAllDeliveryStaff();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deliveryStaffService.deleteDeliveryStaff(id);
        return ResponseEntity.noContent().build();
    }
}
