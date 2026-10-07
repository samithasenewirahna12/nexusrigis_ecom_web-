package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Order;
import com.sliit.ecommerce.Entitys.Payment;
import com.sliit.ecommerce.dto.PaymentCreateRequest;
import com.sliit.ecommerce.dto.PaymentDTO;
import com.sliit.ecommerce.exception.BusinessRuleException;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.DeliveryRepository;
import com.sliit.ecommerce.repository.OrderRepository;
import com.sliit.ecommerce.repository.PaymentRepository;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ModelMapper modelMapper;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, DeliveryRepository deliveryRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.deliveryRepository = deliveryRepository;
    }

    // create a new payment
    public PaymentDTO createPayment(PaymentCreateRequest request) {

        // find the order
        Optional<Order> orderResult = orderRepository.findById(request.getOrderId());

        if (orderResult.isEmpty()) {
            throw ResourceNotFoundException.of("Order", request.getOrderId());
        }

        Order order = orderResult.get();

        // check whether the order already has a payment
        Optional<Payment> existingPayment = paymentRepository.findByOrder_OrderId(order.getOrderId());

        if (existingPayment.isPresent()) {
            throw new BusinessRuleException("Order " + order.getOrderId() + " already has a payment");
        }

        // update order total amount to match requested payment amount if provided
        if (request.getAmount() != null) {
            order.setTotalAmount(request.getAmount());
        } else if (order.getTotalAmount() == null) {
            throw new BusinessRuleException("Order total amount is not available");
        }

        // create payment
        Payment payment = new Payment();
        payment.setPaymentId(IdGenerator.nextId("PAY", paymentRepository.findAll().stream().map(Payment::getPaymentId).toList()));

        payment.setOrder(order);
        payment.setAmount(request.getAmount() != null ? request.getAmount() : order.getTotalAmount());
        payment.setMethod(request.getMethod());
        payment.setPaymentDate(LocalDate.now());

        boolean isCod = request.getMethod() != null &&
                (request.getMethod().equalsIgnoreCase("Cash on Delivery") || request.getMethod().equalsIgnoreCase("COD"));

        if (isCod) {
            payment.setStatus("PENDING");
            order.setStatus("PENDING");
        } else {
            payment.setStatus("COMPLETED");
            order.setStatus("PENDING");
        }

        // save payment & order
        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);

        // convert to DTO
        PaymentDTO paymentDTO = toPaymentDTO(savedPayment);

        return paymentDTO;
    }

    // get payment by ID
    @Transactional(readOnly = true)
    public PaymentDTO getPayment(String id) {

        Optional<Payment> paymentResult = paymentRepository.findById(id);

        if (paymentResult.isEmpty()) {
            throw ResourceNotFoundException.of("Payment", id);
        }

        Payment payment = paymentResult.get();

        PaymentDTO paymentDTO = toPaymentDTO(payment);

        return paymentDTO;
    }

    // get payment by order ID
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentByOrder(String orderId) {

        Optional<Payment> paymentResult = paymentRepository.findByOrder_OrderId(orderId);

        if (paymentResult.isEmpty()) {
            throw new ResourceNotFoundException("Payment not found for order: " + orderId);
        }

        Payment payment = paymentResult.get();

        PaymentDTO paymentDTO = toPaymentDTO(payment);

        return paymentDTO;
    }
    private PaymentDTO toPaymentDTO(Payment payment) {
        PaymentDTO dto = modelMapper.map(payment, PaymentDTO.class);
        if (payment.getOrder() != null) dto.setOrderId(payment.getOrder().getOrderId());
        return dto;
    }

}