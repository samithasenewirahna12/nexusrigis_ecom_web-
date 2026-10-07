package com.sliit.ecommerce.dto;



import java.time.LocalDate;

public class WarehouseStaffDTO {

    private String userId;

    private String name;

    private String email;

    private String phone;

    private AddressDTO address;

    private LocalDate registeredDate;

    private String warehouseLoc;

    private String userImage;

    public WarehouseStaffDTO() {}

    public WarehouseStaffDTO(String userId, String name, String email, String phone, AddressDTO address, LocalDate registeredDate, String warehouseLoc, String userImage) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.registeredDate = registeredDate;
        this.warehouseLoc = warehouseLoc;
        this.userImage = userImage;
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

    public AddressDTO getAddress() {
        return address;
    }

    public void setAddress(AddressDTO address) {
        this.address = address;
    }

    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(LocalDate registeredDate) {
        this.registeredDate = registeredDate;
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
