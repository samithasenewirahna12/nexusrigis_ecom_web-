package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Coupon;
import com.sliit.ecommerce.dto.CouponCreateRequest;
import com.sliit.ecommerce.dto.CouponDTO;
import com.sliit.ecommerce.exception.BusinessRuleException;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.CouponRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final ModelMapper modelMapper;


    public CouponService(CouponRepository couponRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.couponRepository = couponRepository;
    }


    // create coupon
    public CouponDTO createCoupon(CouponCreateRequest request) {

        // check whether coupon code already exists
        Optional<Coupon> existingCoupon = couponRepository.findByCode(request.getCode());

        if (existingCoupon.isPresent()) {
            throw new BusinessRuleException("Coupon code already exists: " + request.getCode());
        }

        // check coupon dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessRuleException("Coupon endDate cannot be before startDate");
        }

        // create coupon
        Coupon coupon = new Coupon();
        coupon.setCouponId(IdGenerator.nextId("COUP", couponRepository.findAll().stream().map(Coupon::getCouponId).toList()));

            coupon.setCode(request.getCode());
            coupon.setDescription(request.getDescription());
            coupon.setDiscPercent(request.getDiscPercent());
            coupon.setStartDate(request.getStartDate());
            coupon.setEndDate(request.getEndDate());

        // save coupon
        Coupon savedCoupon = couponRepository.save(coupon);

        // convert to DTO
        CouponDTO couponDTO = modelMapper.map(savedCoupon, CouponDTO.class);

        return couponDTO;
    }


    // get coupon by ID
    @Transactional(readOnly = true)
    public CouponDTO getCoupon(String id) {

        Optional<Coupon> result = couponRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Coupon", id);
        }

        Coupon coupon = result.get();

        // convert to DTO
        CouponDTO couponDTO = modelMapper.map(coupon, CouponDTO.class);

        return couponDTO;
    }


    // get coupon by code
    @Transactional(readOnly = true)
    public CouponDTO getCouponByCode(String code) {

        Optional<Coupon> result = couponRepository.findByCode(code);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Coupon not found with code: " + code);
        }

        Coupon coupon = result.get();

        // convert to DTO
        CouponDTO couponDTO = modelMapper.map(coupon, CouponDTO.class);

        return couponDTO;
    }


    // get all coupons
    @Transactional(readOnly = true)
    public List<CouponDTO> getAllCoupons() {

        List<Coupon> coupons = couponRepository.findAll();

        List<CouponDTO> couponDTOList = new ArrayList<>();

        for (Coupon coupon : coupons) {

            CouponDTO couponDTO = modelMapper.map(coupon, CouponDTO.class);
            couponDTOList.add(couponDTO);
        }

        return couponDTOList;
    }


    // update coupon
    public CouponDTO updateCoupon(String id, CouponCreateRequest request) {
        Optional<Coupon> result = couponRepository.findById(id);
        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Coupon", id);
        }
        Coupon coupon = result.get();
        coupon.setCode(request.getCode());
        coupon.setDescription(request.getDescription());
        coupon.setDiscPercent(request.getDiscPercent());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        Coupon updated = couponRepository.save(coupon);
        return modelMapper.map(updated, CouponDTO.class);
    }

    // delete coupon
    public void deleteCoupon(String id) {

        Optional<Coupon> result = couponRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Coupon", id);
        }

        Coupon coupon = result.get();
        couponRepository.delete(coupon);
    }
}