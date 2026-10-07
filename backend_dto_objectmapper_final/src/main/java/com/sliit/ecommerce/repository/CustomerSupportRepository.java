package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.CustomerSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerSupportRepository extends JpaRepository<CustomerSupport, String> {
    List<CustomerSupport> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<CustomerSupport> findAllByOrderByCreatedAtDesc();
}
