package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.WishlistDTO;
import com.sliit.ecommerce.service.WishlistService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers/{customerId}/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public WishlistDTO getWishlist(@PathVariable String customerId) {
        return wishlistService.getWishlist(customerId);
    }

    @PostMapping("/products/{productId}")
    public WishlistDTO addProduct(@PathVariable String customerId, @PathVariable String productId) {
        return wishlistService.addProduct(customerId, productId);
    }

    @DeleteMapping("/products/{productId}")
    public WishlistDTO removeProduct(@PathVariable String customerId, @PathVariable String productId) {
        return wishlistService.removeProduct(customerId, productId);
    }
}
