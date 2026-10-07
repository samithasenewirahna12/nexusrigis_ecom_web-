package com.sliit.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ShoppingCartDTO {

    private String cartId;

    private LocalDate createdDate;

    private String customerId;

    private List<CartItemDTO> items;

    private BigDecimal totalPrice;

    public ShoppingCartDTO() {}

    public ShoppingCartDTO(String cartId, LocalDate createdDate, String customerId, List<CartItemDTO> items) {
        this.cartId = cartId;
        this.createdDate = createdDate;
        this.customerId = customerId;
        this.items = items;
    }

    public ShoppingCartDTO(String cartId, LocalDate createdDate, String customerId, List<CartItemDTO> items, BigDecimal totalPrice) {
        this.cartId = cartId;
        this.createdDate = createdDate;
        this.customerId = customerId;
        this.items = items;
        this.totalPrice = totalPrice;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
