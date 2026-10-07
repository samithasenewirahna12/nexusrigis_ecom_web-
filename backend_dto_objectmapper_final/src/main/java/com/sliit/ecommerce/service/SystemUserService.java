package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Administrator;
import com.sliit.ecommerce.Entitys.DeliveryStaff;
import com.sliit.ecommerce.Entitys.SupportStaff;
import com.sliit.ecommerce.Entitys.User;
import com.sliit.ecommerce.Entitys.WarehouseStaff;
import com.sliit.ecommerce.dto.ChangePasswordRequest;
import com.sliit.ecommerce.dto.SystemUserCreateRequest;
import com.sliit.ecommerce.dto.SystemUserDTO;
import com.sliit.ecommerce.dto.SystemUserUpdateRequest;
import com.sliit.ecommerce.exception.ApiException;
import com.sliit.ecommerce.repository.UserRepository;
import com.sliit.ecommerce.util.IdGenerator;
import com.sliit.ecommerce.util.Role;
import com.sliit.ecommerce.util.StaffRoles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class SystemUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SystemUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* ---------------- READ ---------------- */

    @Transactional(readOnly = true)
    public List<SystemUserDTO> getAll() {
        return userRepository.findAll().stream()
                .filter(this::isStaff)
                .sorted(Comparator.comparing(User::getRegisteredDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SystemUserDTO getById(String id) {
        return toDto(findStaff(id));
    }

    /* ---------------- CREATE ---------------- */

    @Transactional
    public SystemUserDTO create(SystemUserCreateRequest req) {
        Role role = StaffRoles.parse(req.role());
        String email = req.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        User user = switch (role) {
            case ADMINISTRATOR -> {
                Administrator a = new Administrator();
                a.setAccessLevel(required(req.accessLevel(), "Access level"));
                yield a;
            }
            case WAREHOUSE_STAFF -> {
                WarehouseStaff w = new WarehouseStaff();
                w.setWarehouseLoc(required(req.warehouseLoc(), "Warehouse location"));
                yield w;
            }
            case SUPPORT_STAFF -> {
                SupportStaff s = new SupportStaff();
                s.setDepartment(required(req.department(), "Department"));
                yield s;
            }
            case DELIVERY_STAFF -> {
                DeliveryStaff d = new DeliveryStaff();
                d.setVehicleNo(required(req.vehicleNo(), "Vehicle number"));
                d.setLicenseNo(required(req.licenseNo(), "License number"));
                yield d;
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid staff role.");
        };

        user.setUserId(IdGenerator.nextId(prefixFor(role),
                userRepository.findAll().stream().map(User::getUserId).toList()));
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setPhone(req.phone());
        user.setUserImage(req.userImage());
        user.setRole(role);
        user.setRegisteredDate(LocalDate.now());
        user.setEnabled(req.enabled() == null || req.enabled());

        return toDto(userRepository.save(user));
    }

    /* ---------------- UPDATE ---------------- */

    @Transactional
    public SystemUserDTO update(String id, SystemUserUpdateRequest req) {
        User user = findStaff(id);

        if (isSuperAdmin(user)) {
            if (req.accessLevel() != null && !req.accessLevel().equalsIgnoreCase("FULL_ACCESS") && !req.accessLevel().equalsIgnoreCase("SUPER_ADMIN")) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Super Admin access level cannot be changed.");
            }
            if (req.enabled() != null && !req.enabled()) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Super Admin cannot be disabled.");
            }
        }

        if (req.name() != null) {
            user.setName(required(req.name(), "Name"));
        }

        if (req.email() != null) {
            String email = required(req.email(), "Email").toLowerCase();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
            }
            user.setEmail(email);
        }

        if (req.phone() != null) {
            user.setPhone(req.phone().trim());
        }
        if (req.userImage() != null) {
            user.setUserImage(req.userImage().trim());
        }

        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 6) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Password must contain at least 6 characters.");
            }
            user.setPassword(passwordEncoder.encode(req.password()));
        }

        if (req.enabled() != null && req.enabled() != user.isEnabled()) {
            if (!req.enabled()) {
                assertNotLastEnabledAdmin(user);
            }
            user.setEnabled(req.enabled());
        }

        if (user instanceof Administrator a && req.accessLevel() != null) {
            a.setAccessLevel(required(req.accessLevel(), "Access level"));
        } else if (user instanceof WarehouseStaff w && req.warehouseLoc() != null) {
            w.setWarehouseLoc(required(req.warehouseLoc(), "Warehouse location"));
        } else if (user instanceof SupportStaff s && req.department() != null) {
            s.setDepartment(required(req.department(), "Department"));
        } else if (user instanceof DeliveryStaff d) {
            if (req.vehicleNo() != null) d.setVehicleNo(required(req.vehicleNo(), "Vehicle number"));
            if (req.licenseNo() != null) d.setLicenseNo(required(req.licenseNo(), "License number"));
        }

        if (req.address() != null) {
            user.setAddress(req.address());
        }

        return toDto(userRepository.save(user));
    }

    /* ---------------- CHANGE PASSWORD ---------------- */

    @Transactional
    public void changePassword(String id, ChangePasswordRequest req) {
        User user = findStaff(id);

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "New password must be different from the current password.");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    /* ---------------- STATUS ---------------- */

    @Transactional
    public SystemUserDTO setEnabled(String id, boolean enabled) {
        User user = findStaff(id);

        if (isSuperAdmin(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Super Admin status cannot be altered.");
        }

        if (!enabled) {
            assertNotLastEnabledAdmin(user);
        }
        user.setEnabled(enabled);

        return toDto(userRepository.save(user));
    }

    /* ---------------- DELETE ---------------- */

    // Deliberately NOT @Transactional: the repository's own transaction commits
    // inside delete(), so a foreign-key failure is caught right here.
    public void delete(String id) {
        User user = findStaff(id);

        if (isSuperAdmin(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Super Admin cannot be deleted.");
        }

        assertNotLastEnabledAdmin(user);

        try {
            userRepository.delete(user);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This user has linked records and cannot be deleted. Disable the account instead.");
        }
    }

    /* ---------------- HELPERS ---------------- */

    public boolean isSuperAdmin(User u) {
        if (u == null) return false;
        if (u instanceof Administrator a) {
            String level = a.getAccessLevel();
            if (level != null && (level.equalsIgnoreCase("FULL_ACCESS") || level.equalsIgnoreCase("SUPER_ADMIN"))) {
                return true;
            }
        }
        if (u.getUserId() != null && u.getUserId().equalsIgnoreCase("ADM001")) {
            return true;
        }
        if (u.getEmail() != null && u.getEmail().toLowerCase().contains("superadmin")) {
            return true;
        }
        if (u.getName() != null && u.getName().toLowerCase().contains("super admin")) {
            return true;
        }
        return false;
    }

    private boolean isStaff(User u) {
        return u.getRole() != null && u.getRole() != Role.CUSTOMER;
    }

    private User findStaff(String id) {
        return userRepository.findById(id)
                .filter(this::isStaff)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "System user not found."));
    }

    private String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, label + " is required.");
        }
        return value.trim();
    }

  // Never allow the system to end up with zero active administrators.
    private void assertNotLastEnabledAdmin(User target) {
        if (target.getRole() != Role.ADMINISTRATOR || !target.isEnabled()) {
            return;
        }

        long enabledAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMINISTRATOR && u.isEnabled())
                .count();

        if (enabledAdmins <= 1) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "At least one active administrator must remain.");
        }
    }

    private String prefixFor(Role role) {
        return switch (role) {
            case ADMINISTRATOR -> "ADM";
            case WAREHOUSE_STAFF -> "WH";
            case SUPPORT_STAFF -> "SUP";
            case DELIVERY_STAFF -> "DEL";
            default -> "CUS";
        };
    }

    private SystemUserDTO toDto(User u) {
        String accessLevel = null, warehouseLoc = null, department = null, vehicleNo = null, licenseNo = null;

        if (u instanceof Administrator a) {
            accessLevel = a.getAccessLevel();
        } else if (u instanceof WarehouseStaff w) {
            warehouseLoc = w.getWarehouseLoc();
        } else if (u instanceof SupportStaff s) {
            department = s.getDepartment();
        } else if (u instanceof DeliveryStaff d) {
            vehicleNo = d.getVehicleNo();
            licenseNo = d.getLicenseNo();
        }

        return new SystemUserDTO(
                u.getUserId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getUserImage(),
                u.getRole().name(),
                u.isEnabled(),
                u.isEnabled() ? "Active" : "Inactive",
                u.getRegisteredDate(),
                accessLevel, warehouseLoc, department, vehicleNo, licenseNo,
                u.getAddress()
        );
    }
}