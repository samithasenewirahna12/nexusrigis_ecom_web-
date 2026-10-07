package com.sliit.ecommerce.service;


import com.sliit.ecommerce.Entitys.User;
import com.sliit.ecommerce.dto.AdministratorDTO;
import com.sliit.ecommerce.dto.AdministratorUpdateRequest;
import com.sliit.ecommerce.dto.ChangePasswordRequest;
import com.sliit.ecommerce.exception.ApiException;
import com.sliit.ecommerce.repository.UserRepository;
import com.sliit.ecommerce.util.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AdministratorProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdministratorService administratorService;

    public AdministratorProfileService(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       AdministratorService administratorService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.administratorService = administratorService;
    }

    @Transactional
    public AdministratorDTO updateProfile(String id, AdministratorUpdateRequest req) {
        User admin = findStaffUser(id);

        if (req.email() != null && !req.email().trim().isEmpty()) {
            String email = req.email().trim().toLowerCase();
            if (!email.equals(admin.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
            }
            admin.setEmail(email);
        }

        if (req.name() != null && !req.name().trim().isEmpty()) {
            admin.setName(req.name().trim());
        }
        if (req.phone() != null) {
            admin.setPhone(req.phone().trim());
        }
        if (req.userImage() != null && !req.userImage().trim().isEmpty()) {
            admin.setUserImage(req.userImage().trim());
        }
        if (req.address() != null) {
            admin.setAddress(req.address());
        }

        userRepository.save(admin);

        return administratorService.getAdministrator(id);
    }

    @Transactional
    public void changePassword(String id, ChangePasswordRequest req) {
        User admin = findStaffUser(id);

        // 400 (not 401) on purpose: a 401 can make the front end treat the session as expired.
        if (!passwordEncoder.matches(req.currentPassword(), admin.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        if (passwordEncoder.matches(req.newPassword(), admin.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "New password must be different from the current password.");
        }

        admin.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(admin);
    }

    private User findStaffUser(String id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() != null && u.getRole() != Role.CUSTOMER)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff member not found."));
    }
}