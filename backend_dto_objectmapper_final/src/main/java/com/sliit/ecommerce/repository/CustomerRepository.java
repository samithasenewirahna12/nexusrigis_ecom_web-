package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    Optional getCustomerByEmail(String email);

}
