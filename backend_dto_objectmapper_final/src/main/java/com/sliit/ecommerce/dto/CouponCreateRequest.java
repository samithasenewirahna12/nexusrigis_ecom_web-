package com.sliit.ecommerce.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CouponCreateRequest {

    @NotBlank
    private String code;

    private String description;

    @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
    private java.math.BigDecimal discPercent;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    public CouponCreateRequest() {}

    public CouponCreateRequest(String code, java.math.BigDecimal discPercent, LocalDate startDate, LocalDate endDate) {
        this.code = code;
        this.discPercent = discPercent;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.math.BigDecimal getDiscPercent() {
        return discPercent;
    }

    public void setDiscPercent(java.math.BigDecimal discPercent) {
        this.discPercent = discPercent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

}
