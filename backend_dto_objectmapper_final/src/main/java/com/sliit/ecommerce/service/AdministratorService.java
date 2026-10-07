package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Administrator;
import com.sliit.ecommerce.Entitys.User;
import com.sliit.ecommerce.dto.AdministratorCreateRequest;
import com.sliit.ecommerce.dto.AdministratorDTO;
import com.sliit.ecommerce.exception.ApiException;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.AdministratorRepository;
import com.sliit.ecommerce.repository.UserRepository;
import com.sliit.ecommerce.util.IdGenerator;
import com.sliit.ecommerce.util.Role;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdministratorService {

    private final AdministratorRepository administratorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public AdministratorService(AdministratorRepository administratorRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.administratorRepository = administratorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }


    public AdministratorDTO createAdministrator(
            AdministratorCreateRequest request) {

        // Same rules as AuthService, so accounts created here can actually log in
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        if (request.getPassword().length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least 6 characters.");
        }

        Administrator admin = new Administrator();
        admin.setUserId(IdGenerator.nextId("ADM", administratorRepository.findAll().stream().map(Administrator::getUserId).toList()));

        admin.setName(request.getName().trim());
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));   // was stored as plain text
        admin.setPhone(request.getPhone());
        admin.setUserImage(request.getUserImage());
        admin.setRole(Role.ADMINISTRATOR);
        admin.setEnabled(true);

        if (request.getAddress() != null) {
            admin.setAddress(modelMapper.map(request.getAddress(), com.sliit.ecommerce.Entitys.Address.class));
        } else {
            admin.setAddress(null);
        }

        admin.setAccessLevel(request.getAccessLevel());
        admin.setRegisteredDate(LocalDate.now());

        Administrator savedAdmin = administratorRepository.save(admin);

        // Convert entity to DTO
        AdministratorDTO adminDTO = modelMapper.map(savedAdmin, AdministratorDTO.class);

        return adminDTO;
    }


    @Transactional(readOnly = true)
    public AdministratorDTO getAdministrator(String id) {

        Optional<Administrator> result = administratorRepository.findById(id);

        if (result.isPresent()) {
            return modelMapper.map(result.get(), AdministratorDTO.class);
        }

        Optional<User> userResult = userRepository.findById(id);
        if (userResult.isPresent() && userResult.get().getRole() != null && userResult.get().getRole() != Role.CUSTOMER) {
            User u = userResult.get();
            AdministratorDTO dto = new AdministratorDTO();
            dto.setUserId(u.getUserId());
            dto.setName(u.getName());
            dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone());
            dto.setUserImage(u.getUserImage());
            dto.setRegisteredDate(u.getRegisteredDate());
            if (u.getAddress() != null) {
                dto.setAddress(modelMapper.map(u.getAddress(), com.sliit.ecommerce.dto.AddressDTO.class));
            }
            if (u instanceof Administrator a) {
                dto.setAccessLevel(a.getAccessLevel());
            } else {
                dto.setAccessLevel(u.getRole().name());
            }
            return dto;
        }

        throw ResourceNotFoundException.of("Administrator", id);
    }


    @Transactional(readOnly = true)
    public List<AdministratorDTO> getAllAdministrators() {

        List<Administrator> administrators = administratorRepository.findAll();

        List<AdministratorDTO> adminDTOList = new ArrayList<>();

        for (Administrator admin : administrators) {

            AdministratorDTO adminDTO = modelMapper.map(admin, AdministratorDTO.class);
            adminDTOList.add(adminDTO);
        }

        return adminDTOList;
    }


    public void deleteAdministrator(String id) {

        Optional<Administrator> result = administratorRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Administrator", id);
        }

        Administrator admin = result.get();
        administratorRepository.delete(admin);
    }
}