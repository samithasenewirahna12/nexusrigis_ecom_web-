package com.sliit.ecommerce.controller;


import com.sliit.ecommerce.dto.AddToCartRequest;
import com.sliit.ecommerce.dto.ShoppingCartDTO;
import com.sliit.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers/{customerId}/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ShoppingCartDTO getCart(@PathVariable String customerId) {
        return cartService.getCart(customerId);
    }

    @PostMapping("/items")
    public ShoppingCartDTO addItem(@PathVariable String customerId, @Valid @RequestBody AddToCartRequest request) {
        System.out.println(request.getProductId());
        System.out.println(request.getQuantity());
        System.out.println(customerId);
        return cartService.addItem(customerId, request);
    }

    @DeleteMapping("/items/{productId}")
    public ShoppingCartDTO removeItem(@PathVariable String customerId, @PathVariable String productId) {
        return cartService.removeItem(customerId, productId);
    }

    @PutMapping("/items/{productId}")
    public ShoppingCartDTO updateQuantity(
            @PathVariable String customerId,
            @PathVariable String productId,
            @RequestBody java.util.Map<String, Integer> payload) {
        Integer quantity = payload.get("quantity");
        int qty = (quantity != null) ? quantity : 1;
        return cartService.updateItemQuantity(customerId, productId, qty);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@PathVariable String customerId) {
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}
