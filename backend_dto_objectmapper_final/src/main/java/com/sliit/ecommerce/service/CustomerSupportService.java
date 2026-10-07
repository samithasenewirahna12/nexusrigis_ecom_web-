package com.sliit.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sliit.ecommerce.Entitys.CustomerSupport;
import com.sliit.ecommerce.Entitys.User;
import com.sliit.ecommerce.dto.CustomerSupportCreateRequest;
import com.sliit.ecommerce.dto.CustomerSupportDTO;
import com.sliit.ecommerce.dto.SupportMessageDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.CustomerSupportRepository;
import com.sliit.ecommerce.repository.UserRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerSupportService {

    private final CustomerSupportRepository customerSupportRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public CustomerSupportService(CustomerSupportRepository customerSupportRepository, UserRepository userRepository, ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.customerSupportRepository = customerSupportRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public CustomerSupportDTO createInquiry(CustomerSupportCreateRequest request) {
        CustomerSupport inquiry = new CustomerSupport();
        
        List<String> existingIds = customerSupportRepository.findAll()
                .stream()
                .map(CustomerSupport::getInquiryId)
                .collect(Collectors.toList());
                
        inquiry.setInquiryId(IdGenerator.nextId("INQ", existingIds));
        inquiry.setCustomerId(request.getCustomerId());
        inquiry.setCustomerName(request.getCustomerName() != null && !request.getCustomerName().isBlank() ? request.getCustomerName() : "Customer");
        inquiry.setCustomerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : "");
        inquiry.setCustomerPhone(request.getCustomerPhone());
        inquiry.setSubject(request.getSubject());
        inquiry.setMessage(request.getMessage());
        inquiry.setCategory(request.getCategory() != null ? request.getCategory() : "Other");
        inquiry.setPriority(request.getPriority() != null ? request.getPriority() : "Medium");
        inquiry.setStatus("Open");
        inquiry.setCreatedAt(LocalDateTime.now());
        inquiry.setUpdatedAt(LocalDateTime.now());

        // Initialize message thread with the original customer message
        List<SupportMessageDTO> msgList = new ArrayList<>();
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            msgList.add(new SupportMessageDTO("CUSTOMER", inquiry.getCustomerName(), request.getMessage(), inquiry.getCreatedAt()));
        }
        inquiry.setMessagesJson(serializeMessages(msgList));

        CustomerSupport saved = customerSupportRepository.save(inquiry);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerSupportDTO> getAllInquiries(String customerId) {
        List<CustomerSupport> list;
        if (customerId != null && !customerId.isBlank()) {
            list = customerSupportRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else {
            list = customerSupportRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerSupportDTO getInquiryById(String id) {
        CustomerSupport inquiry = customerSupportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerSupport", id));
        return toDTO(inquiry);
    }

    public CustomerSupportDTO postMessage(String id, String replyText, String sender, String senderName, String staffId) {
        CustomerSupport inquiry = customerSupportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerSupport", id));

        List<SupportMessageDTO> msgList = parseMessages(inquiry.getMessagesJson());
        if (msgList.isEmpty() && inquiry.getMessage() != null && !inquiry.getMessage().isBlank()) {
            msgList.add(new SupportMessageDTO("CUSTOMER", inquiry.getCustomerName() != null ? inquiry.getCustomerName() : "Customer", inquiry.getMessage(), inquiry.getCreatedAt() != null ? inquiry.getCreatedAt() : LocalDateTime.now()));
        }

        String actualSender = (sender != null && !sender.isBlank()) ? sender.toUpperCase() : "STAFF";
        
        String name = senderName;
        if (name == null || name.isBlank()) {
            if ("CUSTOMER".equals(actualSender)) {
                name = inquiry.getCustomerName() != null ? inquiry.getCustomerName() : "Customer";
            } else {
                name = inquiry.getAssignedStaffName() != null ? inquiry.getAssignedStaffName() : "Support Staff";
            }
        }

        if (staffId != null && !staffId.isBlank()) {
            Optional<User> uOpt = userRepository.findById(staffId);
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                inquiry.setAssignedStaffId(u.getUserId());
                inquiry.setAssignedStaffName(u.getName());
                if ("STAFF".equals(actualSender) && (senderName == null || senderName.isBlank())) {
                    name = u.getName();
                }
            }
        }

        msgList.add(new SupportMessageDTO(actualSender, name, replyText, LocalDateTime.now()));
        inquiry.setMessagesJson(serializeMessages(msgList));
        inquiry.setReply(replyText);
        inquiry.setRepliedAt(LocalDateTime.now());

        if ("CUSTOMER".equals(actualSender)) {
            inquiry.setStatus("Open");
        } else {
            if ("Open".equalsIgnoreCase(inquiry.getStatus())) {
                inquiry.setStatus("In Progress");
            }
        }

        inquiry.setUpdatedAt(LocalDateTime.now());
        CustomerSupport saved = customerSupportRepository.save(inquiry);
        return toDTO(saved);
    }

    public CustomerSupportDTO assignStaff(String id, String staffId) {
        CustomerSupport inquiry = customerSupportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerSupport", id));

        if (staffId != null && !staffId.isBlank()) {
            Optional<User> userOpt = userRepository.findById(staffId);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                inquiry.setAssignedStaffId(u.getUserId());
                inquiry.setAssignedStaffName(u.getName());
            } else {
                inquiry.setAssignedStaffId(staffId);
                inquiry.setAssignedStaffName("Support Staff");
            }
        } else {
            inquiry.setAssignedStaffId(null);
            inquiry.setAssignedStaffName(null);
        }

        inquiry.setUpdatedAt(LocalDateTime.now());
        CustomerSupport saved = customerSupportRepository.save(inquiry);
        return toDTO(saved);
    }

    public CustomerSupportDTO updateStatus(String id, String status) {
        CustomerSupport inquiry = customerSupportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerSupport", id));

        inquiry.setStatus(status);
        inquiry.setUpdatedAt(LocalDateTime.now());
        CustomerSupport saved = customerSupportRepository.save(inquiry);
        return toDTO(saved);
    }

    public CustomerSupportDTO updatePriority(String id, String priority) {
        CustomerSupport inquiry = customerSupportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerSupport", id));

        inquiry.setPriority(priority);
        inquiry.setUpdatedAt(LocalDateTime.now());
        CustomerSupport saved = customerSupportRepository.save(inquiry);
        return toDTO(saved);
    }

    public void deleteInquiry(String id) {
        if (!customerSupportRepository.existsById(id)) {
            throw ResourceNotFoundException.of("CustomerSupport", id);
        }
        customerSupportRepository.deleteById(id);
    }

    private List<SupportMessageDTO> parseMessages(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SupportMessageDTO>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeMessages(List<SupportMessageDTO> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            return "[]";
        }
    }

    private CustomerSupportDTO toDTO(CustomerSupport entity) {
        CustomerSupportDTO dto = modelMapper.map(entity, CustomerSupportDTO.class);
        List<SupportMessageDTO> msgs = parseMessages(entity.getMessagesJson());
        if (msgs.isEmpty() && entity.getMessage() != null && !entity.getMessage().isBlank()) {
            msgs.add(new SupportMessageDTO("CUSTOMER", entity.getCustomerName() != null ? entity.getCustomerName() : "Customer", entity.getMessage(), entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now()));
            if (entity.getReply() != null && !entity.getReply().isBlank()) {
                msgs.add(new SupportMessageDTO("STAFF", entity.getAssignedStaffName() != null ? entity.getAssignedStaffName() : "Support Representative", entity.getReply(), entity.getRepliedAt() != null ? entity.getRepliedAt() : entity.getCreatedAt()));
            }
        }
        dto.setMessages(msgs);

        if (entity.getCustomerId() != null && !entity.getCustomerId().isBlank()) {
            userRepository.findById(entity.getCustomerId()).ifPresent(user -> {
                dto.setCustomerImage(user.getUserImage());
            });
        }

        return dto;
    }
}
