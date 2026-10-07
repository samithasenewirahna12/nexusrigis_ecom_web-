package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.DealCreateRequest;
import com.sliit.ecommerce.dto.DealDTO;
import com.sliit.ecommerce.service.DealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deals")
public class DealController {
    
    private final DealService dealService;
    
    public DealController(DealService dealService) {
        this.dealService = dealService;
    }
    
    @PostMapping
    public ResponseEntity<DealDTO> create(@Valid @RequestBody DealCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.createDeal(request));
    }
    
    @PostMapping("/bulk")
    public ResponseEntity<List<DealDTO>> createBulk(@Valid @RequestBody DealCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.createDealsBulk(request));
    }
    
    @PutMapping("/{id}")
    public DealDTO update(@PathVariable String id, @Valid @RequestBody DealCreateRequest request) {
        return dealService.updateDeal(id, request);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        dealService.deleteDeal(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    public List<DealDTO> getAll() {
        return dealService.getAllDeals();
    }
}
