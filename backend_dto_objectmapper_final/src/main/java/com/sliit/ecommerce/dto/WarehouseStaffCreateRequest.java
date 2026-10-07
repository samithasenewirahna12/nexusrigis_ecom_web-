package com.sliit.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class WarehouseStaffCreateRequest {

    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    private String phone;

    private AddressDTO address;

    @NotBlank
    private String warehouseLoc;

    private String userImage;

    public WarehouseStaffCreateRequest() {}

    public WarehouseStaffCreateRequest(String name, String email, String password, String phone, AddressDTO address, String warehouseLoc, String userImage) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.warehouseLoc = warehouseLoc;
        this.userImage = userImage;
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

    public AddressDTO getAddress() {
        return address;
    }

    public void setAddress(AddressDTO address) {
        this.address = address;
    }

    public String getWarehouseLoc() {
        return warehouseLoc;
    }

    public void setWarehouseLoc(String warehouseLoc) {
        this.warehouseLoc = warehouseLoc;
    }

    public String getUserImage() {return userImage;}

    public void setUserImage(String userImage) {this.userImage = userImage;}
}
