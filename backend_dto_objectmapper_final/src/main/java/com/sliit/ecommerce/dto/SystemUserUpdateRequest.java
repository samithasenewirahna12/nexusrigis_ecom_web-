package com.sliit.ecommerce.dto;

import com.sliit.ecommerce.Entitys.Address;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// Every field is optional: only non-null fields are changed. Role cannot be changed.
public record SystemUserUpdateRequest(
        @Size(max = 120) String name,
        @Email String email,
        String password,
        String phone,
        String userImage,
        Boolean enabled,
        String accessLevel,
        String warehouseLoc,
        String department,
        String vehicleNo,
        String licenseNo,
        Address address
) {}