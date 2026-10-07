package com.sliit.ecommerce.dto;



import java.math.BigDecimal;
import java.time.LocalDate;

public class CouponDTO {

    private String couponId;

    private String code;

    private String description;

    private BigDecimal discPercent;

    private LocalDate startDate;

    private LocalDate endDate;

    public CouponDTO() {}

    public CouponDTO(String couponId, String code, BigDecimal discPercent, LocalDate startDate, LocalDate endDate) {
        this.couponId = couponId;
        this.code = code;
        this.discPercent = discPercent;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getCouponId() {
        return couponId;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
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

    public BigDecimal getDiscPercent() {
        return discPercent;
    }

    public void setDiscPercent(BigDecimal discPercent) {
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
