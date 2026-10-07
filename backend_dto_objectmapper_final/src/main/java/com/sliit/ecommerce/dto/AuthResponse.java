package com.sliit.ecommerce.dto;

import com.sliit.ecommerce.Entitys.Address;

public class AuthResponse {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String userImage;
    private Address address;
    private String role;
    private boolean enabled;

    public AuthResponse() {
    }

    public AuthResponse(
            String userId,
            String name,
            String email,
            String phone,
            String userImage,
            Address address,
            String role,
            boolean enabled
    ) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.userImage = userImage;
        this.address = address;
        this.role = role;
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}