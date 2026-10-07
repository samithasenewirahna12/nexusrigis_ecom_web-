package com.sliit.ecommerce.dto;



import java.time.LocalDate;
import java.util.List;

public class WishlistDTO {

    private String wishlistId;

    private LocalDate createdDate;

    private String customerId;

    private List<String> productIds;

    public WishlistDTO() {}

    public WishlistDTO(String wishlistId, LocalDate createdDate, String customerId, List<String> productIds) {
        this.wishlistId = wishlistId;
        this.createdDate = createdDate;
        this.customerId = customerId;
        this.productIds = productIds;
    }

    public String getWishlistId() {
        return wishlistId;
    }

    public void setWishlistId(String wishlistId) {
        this.wishlistId = wishlistId;
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

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

}
