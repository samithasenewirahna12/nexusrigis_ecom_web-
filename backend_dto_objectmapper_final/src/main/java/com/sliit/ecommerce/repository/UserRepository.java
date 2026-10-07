package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> getUserByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
