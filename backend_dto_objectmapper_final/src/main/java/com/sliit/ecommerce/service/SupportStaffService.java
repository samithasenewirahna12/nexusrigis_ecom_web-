package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.SupportStaff;
import com.sliit.ecommerce.dto.SupportStaffCreateRequest;
import com.sliit.ecommerce.dto.SupportStaffDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.SupportStaffRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SupportStaffService {

    private final SupportStaffRepository supportStaffRepository;

    private final ModelMapper modelMapper;


    public SupportStaffService(SupportStaffRepository supportStaffRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.supportStaffRepository = supportStaffRepository;
    }

    // create a new support staff member
    public SupportStaffDTO createSupportStaff( SupportStaffCreateRequest request) {

        // create support staff
        SupportStaff staff = new SupportStaff();
        staff.setUserId(IdGenerator.nextId("SS", supportStaffRepository.findAll().stream().map(SupportStaff::getUserId).toList()));

        staff.setName(request.getName());
        staff.setEmail(request.getEmail());
        staff.setPassword(request.getPassword());
        staff.setPhone(request.getPhone());
        staff.setUserImage(request.getUserImage());

        // set address
        if (request.getAddress() != null) {
            staff.setAddress(modelMapper.map(request.getAddress(), com.sliit.ecommerce.Entitys.Address.class));
        } else {
            staff.setAddress(null);
        }

        staff.setDepartment(request.getDepartment());
        staff.setRegisteredDate(LocalDate.now());

        // save support staff
        SupportStaff savedStaff = supportStaffRepository.save(staff);

        // convert to DTO
        SupportStaffDTO staffDTO = modelMapper.map(savedStaff, SupportStaffDTO.class);

        return staffDTO;
    }

    // get support staff by ID
    @Transactional(readOnly = true)
    public SupportStaffDTO getSupportStaff(String id) {

        Optional<SupportStaff> staffResult = supportStaffRepository.findById(id);

        if (staffResult.isEmpty()) {
            throw ResourceNotFoundException.of("SupportStaff", id);
        }

        SupportStaff staff = staffResult.get();

        SupportStaffDTO staffDTO = modelMapper.map(staff, SupportStaffDTO.class);

        return staffDTO;
    }

    // get all support staff
    @Transactional(readOnly = true)
    public List<SupportStaffDTO> getAllSupportStaff() {

        List<SupportStaff> staffList = supportStaffRepository.findAll();

        List<SupportStaffDTO> staffDTOList = new ArrayList<>();

        for (SupportStaff staff : staffList) {

            SupportStaffDTO staffDTO = modelMapper.map(staff, SupportStaffDTO.class);
            staffDTOList.add(staffDTO);
        }

        return staffDTOList;
    }

    // delete support staff
    public void deleteSupportStaff(String id) {

        Optional<SupportStaff> staffResult = supportStaffRepository.findById(id);

        if (staffResult.isEmpty()) {
            throw ResourceNotFoundException.of("SupportStaff", id);
        }

        SupportStaff staff = staffResult.get();

        supportStaffRepository.delete(staff);
    }
}