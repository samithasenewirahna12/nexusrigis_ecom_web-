package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, String> {
    Optional<ShoppingCart> findByCustomer_UserId(String customerId);
}
