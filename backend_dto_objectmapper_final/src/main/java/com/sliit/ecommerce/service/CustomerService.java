package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Customer;
import com.sliit.ecommerce.Entitys.Order;
import com.sliit.ecommerce.dto.CustomerCreateRequest;
import com.sliit.ecommerce.dto.CustomerDTO;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.CustomerRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;


    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.modelMapper = modelMapper;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private CustomerDTO mapToDTO(Customer customer) {
        CustomerDTO dto = modelMapper.map(customer, CustomerDTO.class);
        dto.setUserImage(customer.getUserImage());
        dto.setOrderCount(customer.getOrders() != null ? customer.getOrders().size() : 0);
        double total = 0.0;
        if (customer.getOrders() != null) {
            for (Order order : customer.getOrders()) {
                if (order.getTotalAmount() != null) {
                    total += order.getTotalAmount().doubleValue();
                }
            }
        }
        dto.setTotalSpent(total);
        return dto;
    }

    // create customer
    public CustomerDTO createCustomer(CustomerCreateRequest request) {

        Customer customer = new Customer();
        customer.setUserId(IdGenerator.nextId("C", customerRepository.findAll().stream().map(Customer::getUserId).toList()));

            customer.setName(request.getName());
            customer.setEmail(request.getEmail());
            
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                customer.setPassword(passwordEncoder.encode(request.getPassword()));
            } else if (customer.getPassword() == null) {
                customer.setPassword(passwordEncoder.encode("dummyPassword123")); // Fallback if somehow null on create
            }
            
            customer.setPhone(request.getPhone());
        customer.setUserImage(request.getUserImage());
        // set address
        if (request.getAddress() != null) {
            customer.setAddress(modelMapper.map(request.getAddress(), com.sliit.ecommerce.Entitys.Address.class));
        } else {
            customer.setAddress(null);
        }


        customer.setDob(request.getDob());
        customer.setRegisteredDate(LocalDate.now());

        // new customer starts with 0 loyalty points
        customer.setLoyaltyPoints(0);


        // save customer
        Customer savedCustomer = customerRepository.save(customer);

        // convert to DTO
        return mapToDTO(savedCustomer);
    }


    // get customer by ID
    @Transactional(readOnly = true)
    public CustomerDTO getCustomer(String id) {

        Optional<Customer> result = customerRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Customer", id);
        }

        Customer customer = result.get();

        // convert to DTO
        return mapToDTO(customer);
    }


    // get all customers
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {

        List<Customer> customers = customerRepository.findAll();

        List<CustomerDTO> customerDTOList = new ArrayList<>();

        for (Customer customer : customers) {

            customerDTOList.add(mapToDTO(customer));
        }

        return customerDTOList;
    }


    // update customer
    public CustomerDTO updateCustomer(String id, CustomerCreateRequest request) {

        // find customer
        Optional<Customer> result = customerRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Customer", id);
        }

        Customer customer = result.get();

        // update customer details
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty() && !request.getPassword().equals("dummyPassword123")) {
            customer.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        customer.setPhone(request.getPhone());
        customer.setUserImage(request.getUserImage());

        // update address
        if (request.getAddress() != null) {
            customer.setAddress(modelMapper.map(request.getAddress(), com.sliit.ecommerce.Entitys.Address.class));
        } else {
            customer.setAddress(null);
        }

        customer.setDob(request.getDob());

        // save updated customer
        Customer updatedCustomer = customerRepository.save(customer);

        // convert to DTO
        return mapToDTO(updatedCustomer);
    }


    // delete customer
    public void deleteCustomer(String id) {

        Optional<Customer> result = customerRepository.findById(id);

        if (result.isEmpty()) {

            throw ResourceNotFoundException.of("Customer", id);
        }

        Customer customer = result.get();
        customerRepository.delete(customer);
    }

    public CustomerDTO getCustomerByEmail(String email) {
        Optional<Customer> customerByEmail = customerRepository.getCustomerByEmail(email);
        if (customerByEmail.isEmpty()) {
            throw ResourceNotFoundException.of("Customer", email);
        }
        return mapToDTO(customerByEmail.get());
    }
}
