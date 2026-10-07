package com.sliit.ecommerce.controller;

import com.sliit.ecommerce.dto.CustomerSupportCreateRequest;
import com.sliit.ecommerce.dto.CustomerSupportDTO;
import com.sliit.ecommerce.service.CustomerSupportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class CustomerSupportController {

    private final CustomerSupportService customerSupportService;

    public CustomerSupportController(CustomerSupportService customerSupportService) {
        this.customerSupportService = customerSupportService;
    }

    // GET /api/support/inquiries or /api/support/tickets
    @GetMapping({"/inquiries", "/tickets"})
    public List<CustomerSupportDTO> getAllInquiries(@RequestParam(required = false) String customerId) {
        return customerSupportService.getAllInquiries(customerId);
    }

    // GET /api/support/inquiries/{id} or /api/support/tickets/{id}
    @GetMapping({"/inquiries/{id}", "/tickets/{id}"})
    public CustomerSupportDTO getInquiryById(@PathVariable String id) {
        return customerSupportService.getInquiryById(id);
    }

    // POST /api/support/inquiries or /api/support/tickets
    @PostMapping({"/inquiries", "/tickets"})
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSupportDTO createInquiry(@RequestBody CustomerSupportCreateRequest request) {
        return customerSupportService.createInquiry(request);
    }

    // POST/PUT /api/support/inquiries/{id}/reply
    @RequestMapping(value = {"/inquiries/{id}/reply", "/tickets/{id}/reply", "/inquiries/{id}/message", "/tickets/{id}/message"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public CustomerSupportDTO replyInquiry(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {
        String replyText = payload.getOrDefault("reply", payload.get("message"));
        String sender = payload.getOrDefault("sender", "STAFF");
        String senderName = payload.get("senderName");
        String staffId = payload.get("supportStaffId");
        return customerSupportService.postMessage(id, replyText, sender, senderName, staffId);
    }

    // PUT /api/support/inquiries/{id}/assign
    @PutMapping({"/inquiries/{id}/assign", "/tickets/{id}/assign"})
    public CustomerSupportDTO assignStaff(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {
        String staffId = payload.getOrDefault("supportStaffId", payload.get("assignedStaffId"));
        return customerSupportService.assignStaff(id, staffId);
    }

    // PUT /api/support/inquiries/{id}/status
    @PutMapping({"/inquiries/{id}/status", "/tickets/{id}/status"})
    public CustomerSupportDTO updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        return customerSupportService.updateStatus(id, status);
    }

    // PUT /api/support/inquiries/{id}/priority
    @PutMapping({"/inquiries/{id}/priority", "/tickets/{id}/priority"})
    public CustomerSupportDTO updatePriority(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {
        String priority = payload.get("priority");
        return customerSupportService.updatePriority(id, priority);
    }

    // DELETE /api/support/inquiries/{id}
    @DeleteMapping({"/inquiries/{id}", "/tickets/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInquiry(@PathVariable String id) {
        customerSupportService.deleteInquiry(id);
    }
}
