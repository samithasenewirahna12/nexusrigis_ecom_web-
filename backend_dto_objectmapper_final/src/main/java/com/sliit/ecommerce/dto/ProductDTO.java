package com.sliit.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductDTO {

    private String productId;

    private String name;

    private BigDecimal price;

    private String brand;

    private String discription;

    private Integer stockQty;

    private List<String> images;

    private String categoryId;

    private String categoryName;

    private String adminId;

    private String warehouseStaffId;

    public ProductDTO() {}

    public ProductDTO(String productId, String name, BigDecimal price, String brand,String discription, Integer stockQty, List<String> images, String categoryId, String categoryName) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.brand = brand;
        this.discription = discription;
        this.stockQty = stockQty;
        this.images = images;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getStockQty() {
        return stockQty;
    }

    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDiscription() {
        return discription;
    }

    public void setDiscription(String discription) {
        this.discription = discription;
    }

    public String getDescription() {
        return discription;
    }

    public void setDescription(String description) {
        if (description != null && !description.isBlank()) {
            this.discription = description;
        } else if (this.discription == null) {
            this.discription = description;
        }
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getWarehouseStaffId() {
        return warehouseStaffId;
    }

    public void setWarehouseStaffId(String warehouseStaffId) {
        this.warehouseStaffId = warehouseStaffId;
    }
}
