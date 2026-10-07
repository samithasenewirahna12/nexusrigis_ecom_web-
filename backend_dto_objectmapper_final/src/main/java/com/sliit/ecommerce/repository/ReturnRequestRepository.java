package com.sliit.ecommerce.repository;

import com.sliit.ecommerce.Entitys.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, String> {
    List<ReturnRequest> findByOrder_OrderId(String orderId);
    List<ReturnRequest> findByHandledBy_UserId(String supportStaffId);
    List<ReturnRequest> findByOrder_Customer_UserId(String customerId);
}
