package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.DeliveryStaff;
import com.sliit.ecommerce.dto.DeliveryStaffCreateRequest;
import com.sliit.ecommerce.dto.DeliveryStaffDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.DeliveryStaffRepository;
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
public class DeliveryStaffService {

    private final DeliveryStaffRepository deliveryStaffRepository;


    private final ModelMapper modelMapper;


    public DeliveryStaffService(DeliveryStaffRepository deliveryStaffRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.deliveryStaffRepository = deliveryStaffRepository;
    }

    // create delivery staff
    public DeliveryStaffDTO createDeliveryStaff(DeliveryStaffCreateRequest request) {

        DeliveryStaff staff = new DeliveryStaff();
        staff.setUserId(IdGenerator.nextId("DS", deliveryStaffRepository.findAll().stream().map(DeliveryStaff::getUserId).toList()));

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

        staff.setVehicleNo(request.getVehicleNo());
        staff.setLicenseNo(request.getLicenseNo());
        staff.setRegisteredDate(LocalDate.now());

        // save delivery staff
        DeliveryStaff savedStaff = deliveryStaffRepository.save(staff);

        // convert to DTO
        DeliveryStaffDTO staffDTO = modelMapper.map(savedStaff, DeliveryStaffDTO.class);

        return staffDTO;
    }


    // get delivery staff by ID
    @Transactional(readOnly = true)
    public DeliveryStaffDTO getDeliveryStaff(String id) {

        Optional<DeliveryStaff> result = deliveryStaffRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("DeliveryStaff", id);
        }

        DeliveryStaff staff = result.get();

        // convert to DTO
        DeliveryStaffDTO staffDTO = modelMapper.map(staff, DeliveryStaffDTO.class);

        return staffDTO;
    }


    // get all delivery staff
    @Transactional(readOnly = true)
    public List<DeliveryStaffDTO> getAllDeliveryStaff() {

        List<DeliveryStaff> staffList = deliveryStaffRepository.findAll();

        List<DeliveryStaffDTO> staffDTOList = new ArrayList<>();


        for (DeliveryStaff staff : staffList) {

            DeliveryStaffDTO staffDTO = modelMapper.map(staff, DeliveryStaffDTO.class);
            staffDTOList.add(staffDTO);
        }

        return staffDTOList;
    }


    // delete delivery staff
    public void deleteDeliveryStaff(String id) {

        Optional<DeliveryStaff> result = deliveryStaffRepository.findById(id);

        if (result.isEmpty()) {

            throw ResourceNotFoundException.of("DeliveryStaff", id);
        }

        DeliveryStaff staff = result.get();
        deliveryStaffRepository.delete(staff);
    }
}