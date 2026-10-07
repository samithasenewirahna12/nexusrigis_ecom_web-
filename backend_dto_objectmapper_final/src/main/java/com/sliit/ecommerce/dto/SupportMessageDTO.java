package com.sliit.ecommerce.dto;

import java.time.LocalDateTime;

public class SupportMessageDTO {
    private String sender; // CUSTOMER or STAFF
    private String senderName;
    private String message;
    private LocalDateTime createdAt;

    public SupportMessageDTO() {
    }

    public SupportMessageDTO(String sender, String senderName, String message, LocalDateTime createdAt) {
        this.sender = sender;
        this.senderName = senderName;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
