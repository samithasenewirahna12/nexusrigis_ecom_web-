package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, String> {

    List<PaymentCard> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(String customerId);

    List<PaymentCard> findByCustomerId(String customerId);

    long countByCustomerId(String customerId);
}