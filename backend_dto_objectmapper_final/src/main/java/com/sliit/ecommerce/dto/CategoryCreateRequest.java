package com.sliit.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryCreateRequest {

    @NotBlank
    private String categoryName;

    private String description;

    private String adminId;

    private String categoryImage;

    public CategoryCreateRequest() {}

    public CategoryCreateRequest(String categoryName, String description, String adminId, String categoryImage) {
        this.categoryName = categoryName;
        this.description = description;
        this.adminId = adminId;
        this.categoryImage = categoryImage;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getCategoryImage() {
        return categoryImage;
    }

    public void setCategoryImage(String categoryImage) {
        this.categoryImage = categoryImage;
    }

}
