package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, String> {
}
