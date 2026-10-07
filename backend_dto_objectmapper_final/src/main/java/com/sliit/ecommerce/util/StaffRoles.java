package com.sliit.ecommerce.util;

import com.sliit.ecommerce.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class StaffRoles {

    private StaffRoles() {}

    // Accepts ADMINISTRATOR (and the alias ADMIN). Rejects CUSTOMER and unknown values.
    public static Role parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Staff role is required.");
        }

        String value = raw.trim().toUpperCase().replace(' ', '_');
        if (value.equals("ADMIN")) {
            value = "ADMINISTRATOR";
        }

        Role role;
        try {
            role = Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid staff role.");
        }

        if (role == Role.CUSTOMER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid staff role.");
        }
        return role;
    }
}