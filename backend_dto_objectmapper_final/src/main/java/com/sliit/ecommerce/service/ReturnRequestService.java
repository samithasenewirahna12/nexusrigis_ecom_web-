package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Order;
import com.sliit.ecommerce.Entitys.ReturnRequest;
import com.sliit.ecommerce.Entitys.SupportStaff;
import com.sliit.ecommerce.Entitys.User;
import com.sliit.ecommerce.dto.ReturnRequestCreateRequest;
import com.sliit.ecommerce.dto.ReturnRequestDTO;
import com.sliit.ecommerce.exception.BusinessRuleException;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.OrderRepository;
import com.sliit.ecommerce.repository.ReturnRequestRepository;
import com.sliit.ecommerce.repository.SupportStaffRepository;
import com.sliit.ecommerce.repository.UserRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final SupportStaffRepository supportStaffRepository;
    private final UserRepository userRepository;

    private final ModelMapper modelMapper;


    public ReturnRequestService(ReturnRequestRepository returnRequestRepository, OrderRepository orderRepository, SupportStaffRepository supportStaffRepository, UserRepository userRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.supportStaffRepository = supportStaffRepository;
        this.userRepository = userRepository;
    }

    // create a new return request
    public ReturnRequestDTO createReturnRequest(ReturnRequestCreateRequest request) {

        // find order
        Optional<Order> orderResult = orderRepository.findById(request.getOrderId());

        if (orderResult.isEmpty()) {
            throw ResourceNotFoundException.of("Order", request.getOrderId());
        }

        Order order = orderResult.get();

        // Check if an active (non-rejected) return request already exists for this order
        List<ReturnRequest> existingRequests = returnRequestRepository.findByOrder_OrderId(request.getOrderId());
        boolean hasActiveRequest = existingRequests.stream()
                .anyMatch(r -> r.getStatus() == null || !"REJECTED".equalsIgnoreCase(r.getStatus()));

        if (hasActiveRequest) {
            throw new BusinessRuleException("Order " + request.getOrderId() + " already has an active return request. New requests can only be submitted if previous requests were rejected.");
        }

        // create return request
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setReturnId(IdGenerator.nextId("RET", returnRequestRepository.findAll().stream().map(ReturnRequest::getReturnId).toList()));

        returnRequest.setOrder(order);
        returnRequest.setReason(request.getReason());
        returnRequest.setDescription(request.getDescription());
        returnRequest.setStatus("REQUESTED");
        returnRequest.setRequestDate(LocalDate.now());

        if (request.getRefundAmount() != null) {
            returnRequest.setRefundAmount(request.getRefundAmount());
        } else if (order.getTotalAmount() != null) {
            returnRequest.setRefundAmount(order.getTotalAmount());
        }

        if (request.getEvidenceImage() != null && !request.getEvidenceImage().isBlank()) {
            returnRequest.setEvidenceImage(request.getEvidenceImage());
        }

        // persist optional fields
        if (request.getRefundMethod() != null && !request.getRefundMethod().isBlank()) {
            returnRequest.setRefundMethod(request.getRefundMethod());
        }

        // save return request
        ReturnRequest savedReturnRequest = returnRequestRepository.save(returnRequest);

        // convert to DTO
        ReturnRequestDTO returnRequestDTO = toReturnRequestDTO(savedReturnRequest);

        return returnRequestDTO;
    }

    // get return request by ID
    @Transactional(readOnly = true)
    public ReturnRequestDTO getReturnRequest(String id) {

        Optional<ReturnRequest> returnResult = returnRequestRepository.findById(id);

        if (returnResult.isEmpty()) {
            throw ResourceNotFoundException.of("ReturnRequest", id);
        }

        ReturnRequest returnRequest = returnResult.get();

        ReturnRequestDTO returnRequestDTO = toReturnRequestDTO(returnRequest);

        return returnRequestDTO;
    }

    // get return requests by order
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getReturnsByOrder(String orderId) {

        List<ReturnRequest> returnRequests = returnRequestRepository.findByOrder_OrderId(orderId);

        List<ReturnRequestDTO> returnRequestDTOList = new ArrayList<>();

        for (ReturnRequest returnRequest : returnRequests) {

            ReturnRequestDTO returnRequestDTO = toReturnRequestDTO(returnRequest);
            returnRequestDTOList.add(returnRequestDTO);
        }

        return returnRequestDTOList;
    }

    // get return requests handled by support staff
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getReturnsHandledBy(String supportStaffId) {

        List<ReturnRequest> returnRequests = returnRequestRepository.findByHandledBy_UserId(supportStaffId);

        List<ReturnRequestDTO> returnRequestDTOList = new ArrayList<>();

        for (ReturnRequest returnRequest : returnRequests) {

            ReturnRequestDTO returnRequestDTO = toReturnRequestDTO(returnRequest);
            returnRequestDTOList.add(returnRequestDTO);
        }

        return returnRequestDTOList;
    }

    // get all return requests
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getAllReturns() {
        List<ReturnRequest> returnRequests = returnRequestRepository.findAll();
        List<ReturnRequestDTO> returnRequestDTOList = new ArrayList<>();
        for (ReturnRequest returnRequest : returnRequests) {
            ReturnRequestDTO returnRequestDTO = toReturnRequestDTO(returnRequest);
            returnRequestDTOList.add(returnRequestDTO);
        }
        return returnRequestDTOList;
    }

    // get return requests for a specific customer (by their userId)
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getReturnsByCustomer(String customerId) {
        List<ReturnRequest> returnRequests = returnRequestRepository.findByOrder_Customer_UserId(customerId);
        List<ReturnRequestDTO> dtos = new ArrayList<>();
        for (ReturnRequest returnRequest : returnRequests) {
            dtos.add(toReturnRequestDTO(returnRequest));
        }
        return dtos;
    }

    // handle a return request
    public ReturnRequestDTO handleReturn( String id, String supportStaffId, String status, BigDecimal refundAmount) {

        // find return request
        Optional<ReturnRequest> returnResult = returnRequestRepository.findById(id);

        if (returnResult.isEmpty()) {
            throw ResourceNotFoundException.of("ReturnRequest", id);
        }

        ReturnRequest returnRequest = returnResult.get();

        // find support staff or administrator user
        Optional<User> userResult = userRepository.findById(supportStaffId);
        User staff = null;

        if (userResult.isPresent()) {
            staff = userResult.get();
        } else {
            Optional<SupportStaff> supportStaffResult = supportStaffRepository.findById(supportStaffId);
            if (supportStaffResult.isPresent()) {
                staff = supportStaffResult.get();
            } else {
                throw ResourceNotFoundException.of("User / SupportStaff", supportStaffId);
            }
        }

        // update return request
        returnRequest.setHandledBy(staff);
        returnRequest.setStatus(status);
        returnRequest.setRefundAmount(refundAmount);

        // save updated return request
        ReturnRequest savedReturnRequest = returnRequestRepository.save(returnRequest);

        // convert to DTO
        ReturnRequestDTO returnRequestDTO = toReturnRequestDTO(savedReturnRequest);

        return returnRequestDTO;
    }
    private ReturnRequestDTO toReturnRequestDTO(ReturnRequest entity) {
        ReturnRequestDTO dto = modelMapper.map(entity, ReturnRequestDTO.class);
        if (entity.getOrder() != null) {
            Order order = entity.getOrder();
            dto.setOrderId(order.getOrderId());
            dto.setOrderDate(order.getOrderDate());
            dto.setOrderTotal(order.getTotalAmount());
            if (dto.getRefundAmount() == null && order.getTotalAmount() != null) {
                dto.setRefundAmount(order.getTotalAmount());
            }
            if (order.getCustomer() != null) {
                dto.setCustomerId(order.getCustomer().getUserId());
                dto.setCustomerName(order.getCustomer().getName());
                dto.setCustomerEmail(order.getCustomer().getEmail());
            }
            List<com.sliit.ecommerce.dto.OrderItemDTO> itemDTOs = new ArrayList<>();
            if (order.getItems() != null) {
                for (com.sliit.ecommerce.Entitys.OrderItem item : order.getItems()) {
                    com.sliit.ecommerce.dto.OrderItemDTO itemDTO = modelMapper.map(item, com.sliit.ecommerce.dto.OrderItemDTO.class);
                    if (item.getProduct() != null) {
                        itemDTO.setProductId(item.getProduct().getProductId());
                        itemDTO.setProductName(item.getProduct().getName());
                    }
                    itemDTOs.add(itemDTO);
                }
            }
            dto.setItems(itemDTOs);
        }
        if (entity.getHandledBy() != null) {
            dto.setSupportStaffId(entity.getHandledBy().getUserId());
        }
        dto.setDescription(entity.getDescription());
        dto.setEvidenceImage(entity.getEvidenceImage());
        return dto;
    }

}