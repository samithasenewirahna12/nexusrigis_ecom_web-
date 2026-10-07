package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    Optional<Delivery> findByOrder_OrderId(String orderId);
    List<Delivery> findByDeliveryStaff_UserId(String deliveryStaffId);
}
