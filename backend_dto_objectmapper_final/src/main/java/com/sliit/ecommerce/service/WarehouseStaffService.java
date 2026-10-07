package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.WarehouseStaff;
import com.sliit.ecommerce.dto.WarehouseStaffCreateRequest;
import com.sliit.ecommerce.dto.WarehouseStaffDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.WarehouseStaffRepository;
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
public class WarehouseStaffService {

    private final WarehouseStaffRepository warehouseStaffRepository;

    private final ModelMapper modelMapper;


    public WarehouseStaffService(WarehouseStaffRepository warehouseStaffRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.warehouseStaffRepository = warehouseStaffRepository;
    }

    // create a new warehouse staff member
    public WarehouseStaffDTO createWarehouseStaff( WarehouseStaffCreateRequest request) {

        // create warehouse staff
        WarehouseStaff staff = new WarehouseStaff();
        staff.setUserId(IdGenerator.nextId("WS", warehouseStaffRepository.findAll().stream().map(WarehouseStaff::getUserId).toList()));

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

        staff.setWarehouseLoc(request.getWarehouseLoc());
        staff.setRegisteredDate(LocalDate.now());

        // save warehouse staff
        WarehouseStaff savedStaff = warehouseStaffRepository.save(staff);

        // convert to DTO
        WarehouseStaffDTO staffDTO = modelMapper.map(savedStaff, WarehouseStaffDTO.class);

        return staffDTO;
    }

    // get warehouse staff by ID
    @Transactional(readOnly = true)
    public WarehouseStaffDTO getWarehouseStaff(String id) {

        Optional<WarehouseStaff> staffResult = warehouseStaffRepository.findById(id);

        if (staffResult.isEmpty()) {
            throw ResourceNotFoundException.of("WarehouseStaff", id);
        }

        WarehouseStaff staff = staffResult.get();

        WarehouseStaffDTO staffDTO = modelMapper.map(staff, WarehouseStaffDTO.class);

        return staffDTO;
    }

    // get all warehouse staff
    @Transactional(readOnly = true)
    public List<WarehouseStaffDTO> getAllWarehouseStaff() {

        List<WarehouseStaff> staffList = warehouseStaffRepository.findAll();

        List<WarehouseStaffDTO> staffDTOList = new ArrayList<>();

        for (WarehouseStaff staff : staffList) {

            WarehouseStaffDTO staffDTO = modelMapper.map(staff, WarehouseStaffDTO.class);
            staffDTOList.add(staffDTO);
        }

        return staffDTOList;
    }

    // delete warehouse staff
    public void deleteWarehouseStaff(String id) {

        Optional<WarehouseStaff> staffResult = warehouseStaffRepository.findById(id);

        if (staffResult.isEmpty()) {
            throw ResourceNotFoundException.of("WarehouseStaff", id);
        }

        WarehouseStaff staff = staffResult.get();

        warehouseStaffRepository.delete(staff);
    }
}