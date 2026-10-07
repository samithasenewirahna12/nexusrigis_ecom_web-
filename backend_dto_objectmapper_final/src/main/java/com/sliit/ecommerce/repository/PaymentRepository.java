package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrder_OrderId(String orderId);
}
