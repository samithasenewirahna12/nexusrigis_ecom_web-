package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Address;
import com.sliit.ecommerce.Entitys.Administrator;
import com.sliit.ecommerce.Entitys.Customer;
import com.sliit.ecommerce.Entitys.DeliveryStaff;
import com.sliit.ecommerce.Entitys.SupportStaff;
import com.sliit.ecommerce.Entitys.User;
import com.sliit.ecommerce.Entitys.WarehouseStaff;
import com.sliit.ecommerce.dto.AuthResponse;
import com.sliit.ecommerce.dto.LoginRequest;
import com.sliit.ecommerce.dto.RegisterRequest;
import com.sliit.ecommerce.dto.StaffRegisterRequest;
import com.sliit.ecommerce.exception.ApiException;
import com.sliit.ecommerce.repository.UserRepository;
import com.sliit.ecommerce.util.IdGenerator;
import com.sliit.ecommerce.util.Role;
import com.sliit.ecommerce.util.StaffRoles;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public AuthResponse register(RegisterRequest request) {

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name is required.");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain at least 6 characters.");
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        // Public registration can ONLY create CUSTOMER
        User saved = createAccount(
                Role.CUSTOMER,
                request.getName().trim(),
                email,
                request.getPassword(),
                request.getPhone(),
                request.getUserImage(),
                request.getAddress(),
                true
        );

        return convertToResponse(saved);
    }


    public AuthResponse staffRegister(StaffRegisterRequest request) {

        if (request.role() != null && request.role().toUpperCase().contains("SUPER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SUPER_ADMIN cannot be created through staff registration.");
        }

        Role role = StaffRoles.parse(request.role());

        String email = request.email()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        User saved = createAccount(
                role,
                request.name().trim(),
                email,
                request.password(),
                null,
                null,
                null,
                false
        );

        return convertToResponse(saved);
    }


    public AuthResponse login(LoginRequest request) {
        return convertToResponse(authenticate(request));
    }


    public AuthResponse staffLogin(LoginRequest request) {

        User user = authenticate(request);

        if (user.getRole() == Role.CUSTOMER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not a staff account.");
        }

        return convertToResponse(user);
    }

    private User authenticate(LoginRequest request) {

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {

            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {

            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required.");
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.")
                );

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your account is inactive. Please contact an administrator.");
        }

        return user;
    }


    private User createAccount(
            Role role,
            String name,
            String email,
            String rawPassword,
            String phone,
            String userImage,
            Address address,
            boolean enabled) {

        User user = createUserByRole(role);

        user.setUserId(generateUserId(role));
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setUserImage(userImage);
        user.setAddress(address);
        user.setRole(role);
        user.setRegisteredDate(LocalDate.now());
        user.setEnabled(enabled);

        return userRepository.save(user);
    }


    private User createUserByRole(Role role) {

        return switch (role) {

            case ADMINISTRATOR ->
                    new Administrator();

            case CUSTOMER ->
                    new Customer();

            case WAREHOUSE_STAFF ->
                    new WarehouseStaff();

            case SUPPORT_STAFF ->
                    new SupportStaff();

            case DELIVERY_STAFF ->
                    new DeliveryStaff();
        };
    }


    private String generateUserId(Role role) {

        String prefix = switch (role) {

            case ADMINISTRATOR ->
                    "ADM";

            case CUSTOMER ->
                    "CUS";

            case WAREHOUSE_STAFF ->
                    "WH";

            case SUPPORT_STAFF ->
                    "SUP";

            case DELIVERY_STAFF ->
                    "DEL";
        };

        List<String> existingIds = userRepository.findAll()
                .stream()
                .map(User::getUserId)
                .toList();

        return IdGenerator.nextId(prefix, existingIds);
    }


    private AuthResponse convertToResponse(User user) {

        return new AuthResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getUserImage(),
                user.getAddress(),
                user.getRole().name(),
                user.isEnabled()
        );
    }
}