package com.sliit.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DeliveryDTO {

    private String deliveryId;
    private LocalDate deliveryDate;
    private String status;

    // Order info
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDate assignedDate;

    // Scheduled / expected delivery date (shown in Delivery Management "Expected" column)
    private LocalDate expectedDate;

    // Actual delivery completion date (only set when status becomes "Delivered")
    private LocalDate deliveredDate;

    // Customer info
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerImage;
    private String address;
    private String city;
    private String postalCode;

    // Staff info
    private String deliveryStaffId;
    private String deliveryStaffName;

    public DeliveryDTO() {}

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDate getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDate assignedDate) { this.assignedDate = assignedDate; }

    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }

    public LocalDate getDeliveredDate() { return deliveredDate; }
    public void setDeliveredDate(LocalDate deliveredDate) { this.deliveredDate = deliveredDate; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getDeliveryStaffId() { return deliveryStaffId; }
    public void setDeliveryStaffId(String deliveryStaffId) { this.deliveryStaffId = deliveryStaffId; }

    public String getDeliveryStaffName() { return deliveryStaffName; }
    public void setDeliveryStaffName(String deliveryStaffName) { this.deliveryStaffName = deliveryStaffName; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerImage() { return customerImage; }
    public void setCustomerImage(String customerImage) { this.customerImage = customerImage; }
}
