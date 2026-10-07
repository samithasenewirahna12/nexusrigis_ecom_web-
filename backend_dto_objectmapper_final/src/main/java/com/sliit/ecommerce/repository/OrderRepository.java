package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByCustomer_UserId(String customerId);
}
