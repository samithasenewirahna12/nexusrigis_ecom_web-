package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.DeliveryDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.service.DeliveryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }



    @GetMapping
    public ResponseEntity<List<DeliveryDTO>> getAll(
            @RequestParam(required = false) String deliveryStaffId) {

        if (deliveryStaffId != null && !deliveryStaffId.isBlank()) {
            return ResponseEntity.ok(deliveryService.getDeliveriesByStaff(deliveryStaffId));
        }
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }


    @GetMapping("/{id}")
    public ResponseEntity<DeliveryDTO> get(@PathVariable String id) {
        return ResponseEntity.ok(deliveryService.getDelivery(id));
    }


    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryDTO> getByOrder(@PathVariable String orderId) {
        try {
            return ResponseEntity.ok(deliveryService.getDeliveryByOrder(orderId));
        } catch (ResourceNotFoundException e) {
            // only "not found" is a 404; real errors now surface as 500s instead of being hidden
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping
    public ResponseEntity<DeliveryDTO> assign(
            @RequestParam String orderId,
            @RequestParam String deliveryStaffId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryService.assignDelivery(orderId, deliveryStaffId));
    }


    @PostMapping("/assign-order")
    public ResponseEntity<DeliveryDTO> assignOrderStaff(
            @RequestParam String orderId,
            @RequestParam(required = false, defaultValue = "Delivery Staff") String staffName,
            @RequestParam(required = false, defaultValue = "In Transit") String status,
            @RequestParam(required = false) String expectedDate) {
        DeliveryDTO dto = deliveryService.assignOrderStaff(orderId, staffName, status);
        // If the caller supplied a custom expected date, apply it immediately
        if (expectedDate != null && !expectedDate.isBlank()) {
            dto = deliveryService.updateExpectedDate(dto.getDeliveryId(), expectedDate);
        }
        return ResponseEntity.ok(dto);
    }


    @PutMapping("/{id}/assign")
    public ResponseEntity<DeliveryDTO> assignByName(
            @PathVariable String id,
            @RequestParam String staffName) {
        return ResponseEntity.ok(deliveryService.assignByStaffName(id, staffName));
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryDTO> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, status));
    }


    @PutMapping("/{id}/expected-date")
    public ResponseEntity<DeliveryDTO> updateExpectedDate(
            @PathVariable String id,
            @RequestParam String expectedDate) {
        return ResponseEntity.ok(deliveryService.updateExpectedDate(id, expectedDate));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deliveryService.deleteDelivery(id);
        return ResponseEntity.noContent().build();
    }
}