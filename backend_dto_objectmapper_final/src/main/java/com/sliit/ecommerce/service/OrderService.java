package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.*;
import com.sliit.ecommerce.dto.OrderCreateRequest;
import com.sliit.ecommerce.dto.OrderDTO;
import com.sliit.ecommerce.dto.OrderItemRequest;
import com.sliit.ecommerce.exception.BusinessRuleException;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.*;
import com.sliit.ecommerce.util.IdGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final DeliveryRepository deliveryRepository;
    private final ModelMapper modelMapper;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository, ProductRepository productRepository, CouponRepository couponRepository, DeliveryRepository deliveryRepository, ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.deliveryRepository = deliveryRepository;
    }


    // create order
    public OrderDTO createOrder(OrderCreateRequest request) {

        // find customer
        Optional<Customer> customerResult = customerRepository.findById(request.getCustomerId());

        if (customerResult.isEmpty()) {
            throw ResourceNotFoundException.of("Customer", request.getCustomerId());
        }

        Customer customer = customerResult.get();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("An order must contain at least one item");
        }

        // create order
        Order order = new Order();
        order.setOrderId(IdGenerator.nextId("ORD", orderRepository.findAll().stream().map(Order::getOrderId).toList()));

        order.setCustomer(customer);
        order.setOrderDate(LocalDate.now());
        order.setStatus("Pending");

        // start total price from zero
        BigDecimal total = BigDecimal.ZERO;

        int lineNumber = 1;


        // add products to order
        for (OrderItemRequest itemRequest : request.getItems()) {

            // find product
            Optional<Product> productResult = productRepository.findById(itemRequest.getProductId());

            if (productResult.isEmpty()) {
                throw ResourceNotFoundException.of("Product", itemRequest.getProductId());
            }

            Product product = productResult.get();

            // check stock
            if (product.getStockQty() < itemRequest.getQuantity()) {
                throw new BusinessRuleException("Insufficient stock for product " + product.getProductId());
            }

            // reduce product stock
            int remainingStock = product.getStockQty() - itemRequest.getQuantity();

            product.setStockQty(remainingStock);

            productRepository.save(product);


            // create order item
            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setLineNo(lineNumber);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());

            // add item to order
            order.getItems().add(orderItem);

            // calculate item total
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf( itemRequest.getQuantity()));

            // add item price to total
            total = total.add(itemTotal);

            lineNumber++;
        }

        // apply coupons
        if (request.getCouponCodes() != null) {

            for (String code : request.getCouponCodes()) {

                // Find coupon
                Optional<Coupon> couponResult = couponRepository.findByCode(code);

                if (couponResult.isEmpty()) {
                    throw new ResourceNotFoundException("Coupon not found with code: " + code);
                }

                Coupon coupon = couponResult.get();

                // get today's date
                LocalDate today = LocalDate.now();

                // check coupon start date
                if (coupon.getStartDate() != null) {

                    if (coupon.getStartDate().isAfter(today)) {
                        throw new BusinessRuleException("Coupon " + code + " is not active yet");
                    }
                }

                // check coupon end date
                if (coupon.getEndDate() != null) {

                    if (coupon.getEndDate().isBefore(today)) {
                        throw new BusinessRuleException("Coupon " + code + " has expired");
                    }
                }

                // add coupon to order
                order.getCoupons().add(coupon);

                // calculate discount
                if (coupon.getDiscPercent() != null) {

                    BigDecimal discount = total.multiply(coupon.getDiscPercent()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

                    // remove discount from total
                    total = total.subtract(discount);
                }
            }
        }


        // never negative, always 2 decimals
        if (total.signum() < 0) total = BigDecimal.ZERO;
        order.setTotalAmount(total.setScale(2, java.math.RoundingMode.HALF_UP));

        // save order
        Order savedOrder = orderRepository.save(order);

        // convert order to DTO
        OrderDTO orderDTO = toOrderDTO(savedOrder);

        return orderDTO;
    }


    // get order by ID
    @Transactional(readOnly = true)
    public OrderDTO getOrder(String id) {

        Optional<Order> result = orderRepository.findById(id);

        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Order", id);
        }

        Order order = result.get();

        // convert to DTO
        OrderDTO orderDTO = toOrderDTO(order);
        return orderDTO;
    }

    // get all orders
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {

        List<Order> orders = orderRepository.findAll();

        List<OrderDTO> orderDTOList = new ArrayList<>();

        for (Order order : orders) {

            OrderDTO orderDTO = toOrderDTO(order);
            orderDTOList.add(orderDTO);
        }

        return orderDTOList;
    }


    // get orders by customer
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomer(String customerId) {

        List<Order> orders = orderRepository.findByCustomer_UserId(customerId);

        List<OrderDTO> orderDTOList = new ArrayList<>();

        for (Order order : orders) {

            OrderDTO orderDTO = toOrderDTO(order);
            orderDTOList.add(orderDTO);
        }

        return orderDTOList;
    }


    // update order status
    public OrderDTO updateStatus(String id, String status) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", id));

        String previousStatus = order.getStatus();
        String normalizedStatus = normalizeOrderStatus(status);

        boolean wasCancelled = "Cancelled".equalsIgnoreCase(previousStatus);
        boolean nowCancelled = "Cancelled".equalsIgnoreCase(normalizedStatus);

        if (wasCancelled && !nowCancelled) {
            throw new BusinessRuleException("Order " + id + " is cancelled and cannot be changed");
        }
        if ("Delivered".equalsIgnoreCase(previousStatus) && nowCancelled) {
            throw new BusinessRuleException("Order " + id + " is already delivered and cannot be cancelled");
        }

        // Give the stock back exactly once when an order is cancelled
        if (nowCancelled && !wasCancelled) {
            restoreStock(order);
        }

        order.setStatus(normalizedStatus);
        Order updatedOrder = orderRepository.save(order);

        // Keep Delivery record in sync
        syncDeliveryForOrder(updatedOrder, normalizedStatus);

        return toOrderDTO(updatedOrder);
    }

    private void restoreStock(Order order) {
        if (order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                int qty = item.getQuantity(); // works whether the entity uses int or Integer
                product.setStockQty(product.getStockQty() + qty);
                productRepository.save(product);
            }
        }
    }

    private void syncDeliveryForOrder(Order order, String normalizedStatus) {
        if (order == null || normalizedStatus == null) return;

        // No try/catch here: swallowing a persistence error left the transaction rollback-only
        // and hid the failure. If the delivery can't be synced, the update should fail visibly.
        Optional<Delivery> deliveryOpt = deliveryRepository.findByOrder_OrderId(order.getOrderId());
        Delivery delivery;
        if (deliveryOpt.isPresent()) {
            delivery = deliveryOpt.get();
        } else if ("In Transit".equals(normalizedStatus)
                || "Out for Delivery".equals(normalizedStatus)
                || "Delivered".equals(normalizedStatus)) {
            delivery = new Delivery();
            List<String> ids = new ArrayList<>();
            for (Delivery d : deliveryRepository.findAll()) {
                ids.add(d.getDeliveryId());
            }
            delivery.setDeliveryId(IdGenerator.nextId("DEL", ids));
            delivery.setOrder(order);
            delivery.setDeliveryDate(LocalDate.now().plusDays(3));
        } else {
            return;
        }

        delivery.setStatus(normalizedStatus);
        deliveryRepository.save(delivery);
    }

    private String normalizeOrderStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Pending";
        }
        String clean = status.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        switch (clean) {
            case "PENDING":
                return "Pending";
            case "ORDER_CONFIRMED":
            case "CONFIRMED":
                return "Order Confirmed";
            case "PICKED_AND_PACKED":
            case "PICKED_&_PACKED":
            case "PICKED_PACKED":
            case "PROCESSING":
                return "Picked & Packed";
            case "IN_TRANSIT":
            case "TRANSIT":
            case "SHIPPED":
            case "DISPATCHED":
                return "In Transit";
            case "OUT_FOR_DELIVERY":
            case "OUT_DELIVERY":
                return "Out for Delivery";
            case "DELIVERED":
                return "Delivered";
            case "CANCELLED":
            case "CANCELED":
                return "Cancelled";
            default:
                return status;
        }
    }

    // delete order
    public void deleteOrder(String id) {
        Optional<Order> result = orderRepository.findById(id);
        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Order", id);
        }
        orderRepository.delete(result.get());
    }

    private OrderDTO toOrderDTO(Order order) {
        OrderDTO dto = modelMapper.map(order, OrderDTO.class);
        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getUserId());
            dto.setCustomerName(order.getCustomer().getName());
            dto.setCustomerEmail(order.getCustomer().getEmail());
            dto.setCustomerPhone(order.getCustomer().getPhone());
            dto.setCustomerImage(order.getCustomer().getUserImage());
        }

        List<com.sliit.ecommerce.dto.OrderItemDTO> itemDTOs = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                com.sliit.ecommerce.dto.OrderItemDTO itemDTO = modelMapper.map(item, com.sliit.ecommerce.dto.OrderItemDTO.class);
                if (item.getProduct() != null) {
                    itemDTO.setProductId(item.getProduct().getProductId());
                    itemDTO.setProductName(item.getProduct().getName());
                }
                itemDTOs.add(itemDTO);
            }
        }
        dto.setItems(itemDTOs);

        if (order.getCoupons() != null) {
            dto.setCouponCodes(order.getCoupons().stream()
                    .map(Coupon::getCode)
                    .collect(java.util.stream.Collectors.toList()));
        } else {
            dto.setCouponCodes(new ArrayList<>());
        }
        return dto;
    }

}