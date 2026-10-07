package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByProduct_ProductId(String productId);
    List<Review> findByCustomer_UserId(String customerId);
}
