package com.sliit.ecommerce.controller;


import com.sliit.ecommerce.dto.OrderCreateRequest;
import com.sliit.ecommerce.dto.OrderDTO;
import com.sliit.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDTO> create(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public OrderDTO get(@PathVariable String id) {
        return orderService.getOrder(id);
    }

    @GetMapping
    public List<OrderDTO> getAll(@RequestParam(required = false) String customerId) {
        if (customerId != null && !customerId.isBlank()) {
            return orderService.getOrdersByCustomer(customerId);
        }
        return orderService.getAllOrders();
    }

    // Used by the OrderTrack page ("my orders"); this route did not exist before
    @GetMapping("/customer/{customerId}")
    public List<OrderDTO> getByCustomer(@PathVariable String customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }

    @PutMapping("/{id}/status")
    public OrderDTO updateStatus(@PathVariable String id, @RequestParam String status) {
        return orderService.updateStatus(id, status);
    }

    @PutMapping("/{id}/cancel")
    public OrderDTO cancelOrder(@PathVariable String id) {
        return orderService.updateStatus(id, "Cancelled");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}