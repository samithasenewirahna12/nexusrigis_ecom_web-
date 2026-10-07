package com.sliit.ecommerce.dto;

import java.time.LocalDate;

public class DealDTO {
    private String dealId;
    private String productId;
    private String productName;
    private java.math.BigDecimal productOriginalPrice;
    private String productImage;
    private Integer discountPercentage;
    private String badgeText;
    private LocalDate startDate;
    private LocalDate endDate;

    public DealDTO() {}

    public String getDealId() { return dealId; }
    public void setDealId(String dealId) { this.dealId = dealId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public java.math.BigDecimal getProductOriginalPrice() { return productOriginalPrice; }
    public void setProductOriginalPrice(java.math.BigDecimal productOriginalPrice) { this.productOriginalPrice = productOriginalPrice; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public Integer getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Integer discountPercentage) { this.discountPercentage = discountPercentage; }

    public String getBadgeText() { return badgeText; }
    public void setBadgeText(String badgeText) { this.badgeText = badgeText; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
