package com.sliit.ecommerce.dto;

import com.sliit.ecommerce.Entitys.Address;
import java.time.LocalDate;

public record SystemUserDTO(
        String userId,
        String name,
        String email,
        String phone,
        String userImage,
        String role,            // ADMINISTRATOR | WAREHOUSE_STAFF | SUPPORT_STAFF | DELIVERY_STAFF
        boolean enabled,
        String status,          // "Active" | "Inactive"
        LocalDate registeredDate,
        String accessLevel,     // administrator only
        String warehouseLoc,    // warehouse only
        String department,      // support only
        String vehicleNo,       // delivery only
        String licenseNo,       // delivery only
        Address address
) {}