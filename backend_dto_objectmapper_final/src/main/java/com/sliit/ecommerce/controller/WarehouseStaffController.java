package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.WarehouseStaffCreateRequest;
import com.sliit.ecommerce.dto.WarehouseStaffDTO;
import com.sliit.ecommerce.service.WarehouseStaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse-staff")
public class WarehouseStaffController {

    private final WarehouseStaffService warehouseStaffService;

    public WarehouseStaffController(WarehouseStaffService warehouseStaffService) {
        this.warehouseStaffService = warehouseStaffService;
    }

    @PostMapping
    public ResponseEntity<WarehouseStaffDTO> create(@Valid @RequestBody WarehouseStaffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseStaffService.createWarehouseStaff(request));
    }

    @GetMapping("/{id}")
    public WarehouseStaffDTO get(@PathVariable String id) {
        return warehouseStaffService.getWarehouseStaff(id);
    }

    @GetMapping
    public List<WarehouseStaffDTO> getAll() {
        return warehouseStaffService.getAllWarehouseStaff();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        warehouseStaffService.deleteWarehouseStaff(id);
        return ResponseEntity.noContent().build();
    }
}
