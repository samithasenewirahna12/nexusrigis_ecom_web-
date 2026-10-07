package com.sliit.ecommerce.controller;


import com.sliit.ecommerce.dto.CouponCreateRequest;
import com.sliit.ecommerce.dto.CouponDTO;
import com.sliit.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<CouponDTO> create(@Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @GetMapping("/{id}")
    public CouponDTO get(@PathVariable String id) {
        return couponService.getCoupon(id);
    }

    @GetMapping("/code/{code}")
    public CouponDTO getByCode(@PathVariable String code) {
        return couponService.getCouponByCode(code);
    }

    @GetMapping
    public List<CouponDTO> getAll() {
        return couponService.getAllCoupons();
    }

    @PutMapping("/{id}")
    public CouponDTO update(@PathVariable String id, @Valid @RequestBody CouponCreateRequest request) {
        return couponService.updateCoupon(id, request);
    }

    @PostMapping("/validate")
    public CouponDTO validate(@RequestBody java.util.Map<String, String> body) {
        String code = body != null ? body.get("code") : null;
        return couponService.getCouponByCode(code);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }
}
