package com.sliit.ecommerce.repository;


import com.sliit.ecommerce.Entitys.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {
}
