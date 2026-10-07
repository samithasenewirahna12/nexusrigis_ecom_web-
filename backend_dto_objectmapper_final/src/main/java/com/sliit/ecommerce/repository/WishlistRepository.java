package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, String> {
    Optional<Wishlist> findByCustomer_UserId(String customerId);
}
