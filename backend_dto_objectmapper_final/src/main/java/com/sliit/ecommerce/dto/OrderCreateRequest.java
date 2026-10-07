package com.sliit.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class OrderCreateRequest {

    @NotNull
    private String customerId;

    @NotEmpty @Valid
    private List<OrderItemRequest> items;

    private List<String> couponCodes;

    public OrderCreateRequest() {}

    public OrderCreateRequest(String customerId, List<OrderItemRequest> items, List<String> couponCodes) {
        this.customerId = customerId;
        this.items = items;
        this.couponCodes = couponCodes;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public List<String> getCouponCodes() {
        return couponCodes;
    }

    public void setCouponCodes(List<String> couponCodes) {
        this.couponCodes = couponCodes;
    }

}
