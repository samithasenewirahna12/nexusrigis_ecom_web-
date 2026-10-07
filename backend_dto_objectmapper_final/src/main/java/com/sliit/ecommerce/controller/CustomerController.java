package com.sliit.ecommerce.controller;


import com.sliit.ecommerce.dto.CustomerCreateRequest;
import com.sliit.ecommerce.dto.CustomerDTO;
import com.sliit.ecommerce.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> create(@Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    public CustomerDTO get(@PathVariable String id) {
        return customerService.getCustomer(id);
    }

    @GetMapping("/email/{email}")
    public CustomerDTO getByEmail(@PathVariable String email) {
        return customerService.getCustomerByEmail(email);
    }


    @GetMapping
    public List<CustomerDTO> getAll() {
        return customerService.getAllCustomers();
    }

    @PutMapping("/{id}")
    public CustomerDTO update(@PathVariable String id, @Valid @RequestBody CustomerCreateRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
