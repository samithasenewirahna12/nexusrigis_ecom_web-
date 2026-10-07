package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealRepository extends JpaRepository<Deal, String> {
    List<Deal> findByProduct_ProductId(String productId);
}
