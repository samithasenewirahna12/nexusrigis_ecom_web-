package com.sliit.ecommerce.dto;



import java.time.LocalDate;

public class AdministratorDTO {

    private String userId;

    private String name;

    private String email;

    private String phone;

    private AddressDTO address;

    private LocalDate registeredDate;

    private String userImage;

    private String accessLevel;




    public AdministratorDTO() {}

    public AdministratorDTO(String userId, String name, String email, String phone, AddressDTO address, LocalDate registeredDate,String userImage, String accessLevel) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.registeredDate = registeredDate;
        this.userImage = userImage;
        this.accessLevel = accessLevel;

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

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }


    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

}
