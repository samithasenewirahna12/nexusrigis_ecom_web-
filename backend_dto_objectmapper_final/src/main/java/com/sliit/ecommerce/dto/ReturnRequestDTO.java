package com.sliit.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReturnRequestDTO {

    private String returnId;

    private String reason;

    private String status;

    private LocalDate requestDate;

    private BigDecimal refundAmount;

    private String orderId;

    private String supportStaffId;

    private String refundMethod;

    private String description;

    private String evidenceImage;

    private LocalDate orderDate;

    private BigDecimal orderTotal;

    private String customerId;

    private String customerName;

    private String customerEmail;

    private java.util.List<OrderItemDTO> items;

    public ReturnRequestDTO() {}

    public ReturnRequestDTO(String returnId, String reason, String status, LocalDate requestDate,
                            BigDecimal refundAmount, String orderId, String supportStaffId, String refundMethod) {
        this.returnId = returnId;
        this.reason = reason;
        this.status = status;
        this.requestDate = requestDate;
        this.refundAmount = refundAmount;
        this.orderId = orderId;
        this.supportStaffId = supportStaffId;
        this.refundMethod = refundMethod;
    }

    public String getReturnId() {
        return returnId;
    }

    public void setReturnId(String returnId) {
        this.returnId = returnId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSupportStaffId() {
        return supportStaffId;
    }

    public void setSupportStaffId(String supportStaffId) {
        this.supportStaffId = supportStaffId;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceImage() {
        return evidenceImage;
    }

    public void setEvidenceImage(String evidenceImage) {
        this.evidenceImage = evidenceImage;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public java.util.List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(java.util.List<OrderItemDTO> items) {
        this.items = items;
    }
}
