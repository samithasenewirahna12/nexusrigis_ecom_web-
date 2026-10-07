package com.sliit.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemUserCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank String role,
        String phone,
        String userImage,
        Boolean enabled,        // defaults to true
        String accessLevel,     // required for ADMINISTRATOR
        String warehouseLoc,    // required for WAREHOUSE_STAFF
        String department,      // required for SUPPORT_STAFF
        String vehicleNo,       // required for DELIVERY_STAFF
        String licenseNo        // required for DELIVERY_STAFF
) {}