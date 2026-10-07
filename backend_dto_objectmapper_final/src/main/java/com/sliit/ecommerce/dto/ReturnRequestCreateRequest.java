package com.sliit.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReturnRequestCreateRequest {

    @NotNull
    private String orderId;

    @NotBlank
    private String reason;

    private String description;

    private String refundMethod;

    private java.math.BigDecimal refundAmount;

    private String evidenceImage;

    public ReturnRequestCreateRequest() {}

    public ReturnRequestCreateRequest(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    /**
     * Returns the full reason string, combining reason + description
     * the same way the multipart endpoint does.
     */
    public String getFullReason() {
        String full = reason;
        if (description != null && !description.trim().isEmpty()) {
            full = reason + ": " + description.trim();
        }
        if (full != null && full.length() > 255) {
            full = full.substring(0, 255);
        }
        return full;
    }

    public java.math.BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(java.math.BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getEvidenceImage() {
        return evidenceImage;
    }

    public void setEvidenceImage(String evidenceImage) {
        this.evidenceImage = evidenceImage;
    }
}
