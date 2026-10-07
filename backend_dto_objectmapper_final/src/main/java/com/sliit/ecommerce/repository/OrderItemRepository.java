package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.OrderItem;
import com.sliit.ecommerce.Entitys.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
}
