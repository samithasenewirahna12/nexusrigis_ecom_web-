package com.sliit.ecommerce.dto;

import com.sliit.ecommerce.Entitys.Address;

import java.time.LocalDate;

public class UserDTO {

    private String userId;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String userImage;
    private Address address;
    private LocalDate registeredDate;
    private String role;

    // Required by ModelMapper
    public UserDTO() {
    }

    // Constructor with all fields
    public UserDTO(
            String userId,
            String name,
            String email,
            String password,
            String phone,
            String userImage,
            Address address,
            LocalDate registeredDate) {

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.userImage = userImage;
        this.address = address;
        this.registeredDate = registeredDate;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(LocalDate registeredDate) {
        this.registeredDate = registeredDate;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}