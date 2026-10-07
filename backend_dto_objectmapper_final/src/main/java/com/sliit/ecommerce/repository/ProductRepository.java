package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByCategory_CategoryId(String categoryId);
}
