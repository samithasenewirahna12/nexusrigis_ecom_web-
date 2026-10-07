package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.CartItem;
import com.sliit.ecommerce.Entitys.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {
    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(String cartId, String productId);
}
