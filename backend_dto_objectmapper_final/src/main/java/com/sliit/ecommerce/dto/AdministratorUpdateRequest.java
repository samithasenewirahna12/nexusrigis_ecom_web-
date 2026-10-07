package com.sliit.ecommerce.dto;

import com.sliit.ecommerce.Entitys.Address;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// Sent by the Admin Profile page: PUT /api/administrators/{id}
public record AdministratorUpdateRequest(
        @Size(max = 120) String name,
        @Email String email,
        @Size(max = 20) String phone,
        String userImage,
        Address address
) {}