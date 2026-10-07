package com.sliit.ecommerce.service;

import com.sliit.ecommerce.Entitys.Delivery;
import com.sliit.ecommerce.Entitys.DeliveryStaff;
import com.sliit.ecommerce.Entitys.Order;
import com.sliit.ecommerce.dto.DeliveryDTO;
import com.sliit.ecommerce.exception.BusinessRuleException;
import com.sliit.ecommerce.exception.ResourceNotFoundException;
import com.sliit.ecommerce.repository.DeliveryRepository;
import com.sliit.ecommerce.repository.DeliveryStaffRepository;
import com.sliit.ecommerce.repository.OrderRepository;
import com.sliit.ecommerce.util.IdGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;

    public DeliveryService(DeliveryRepository deliveryRepository, OrderRepository orderRepository, DeliveryStaffRepository deliveryStaffRepository) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.deliveryStaffRepository = deliveryStaffRepository;
    }


    @Transactional(readOnly = true)
    public List<DeliveryDTO> getAllDeliveries() {
        List<Delivery> deliveries = deliveryRepository.findAll();
        List<DeliveryDTO> result = new ArrayList<>();
        java.util.Set<String> processedOrderIds = new java.util.HashSet<>();

        for (Delivery delivery : deliveries) {
            if (delivery.getOrder() != null) {
                processedOrderIds.add(delivery.getOrder().getOrderId());
            }
            result.add(toDeliveryDTO(delivery));
        }

        // Include orders that don't have a delivery record yet
        List<Order> allOrders = orderRepository.findAll();
        for (Order order : allOrders) {
            if (!processedOrderIds.contains(order.getOrderId())) {
                result.add(createPendingDeliveryDTOFromOrder(order));
            }
        }
        return result;
    }


    // HELPERS
    private boolean isBeforeTransit(String status) {
        if (status == null || status.isBlank()) return true;
        String s = status.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return s.equals("PENDING") || s.equals("ASSIGNED") || s.equals("ORDER_CONFIRMED")
                || s.equals("CONFIRMED") || s.equals("PICKED_AND_PACKED") || s.equals("PICKED_&_PACKED")
                || s.equals("PICKED_PACKED") || s.equals("PROCESSING");
    }

    private Delivery newDeliveryFor(Order order) {
        Delivery delivery = new Delivery();
        List<String> ids = new ArrayList<>();
        for (Delivery d : deliveryRepository.findAll()) {
            ids.add(d.getDeliveryId());
        }
        delivery.setDeliveryId(IdGenerator.nextId("DEL", ids));
        delivery.setOrder(order);
        // Default SLA: 5 business days from now
        delivery.setDeliveryDate(LocalDate.now().plusDays(5));
        return delivery;
    }

    private Delivery findOrCreateByOrder(Order order) {
        return deliveryRepository.findByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> newDeliveryFor(order));
    }

    // Resolves a delivery by its id, or by the virtual "DEL-<orderId>" id used for orders without a delivery yet.
    private Delivery resolveDelivery(String id) {
        Optional<Delivery> result = deliveryRepository.findById(id);
        if (result.isPresent()) return result.get();

        if (id != null && id.startsWith("DEL-")) {
            Optional<Order> orderOpt = orderRepository.findById(id.substring(4));
            if (orderOpt.isPresent()) {
                return findOrCreateByOrder(orderOpt.get());
            }
        }
        throw ResourceNotFoundException.of("Delivery", id);
    }

    private void assertOrderActive(Order order) {
        String st = order.getStatus() == null ? "" : order.getStatus().trim();
        if ("Cancelled".equalsIgnoreCase(st) || "Canceled".equalsIgnoreCase(st)) {
            throw new BusinessRuleException("Order " + order.getOrderId() + " is cancelled and cannot be delivered");
        }
    }

    private DeliveryStaff findStaffByNameOrId(String value) {
        if (value == null || value.isBlank()) return null;
        String key = value.trim();
        for (DeliveryStaff s : deliveryStaffRepository.findAll()) {
            if ((s.getName() != null && s.getName().equalsIgnoreCase(key))
                    || (s.getUserId() != null && s.getUserId().equalsIgnoreCase(key))) {
                return s;
            }
        }
        return null;
    }


    // ASSIGN DELIVERY STAFF TO ORDER (by staff id)
    public DeliveryDTO assignDelivery(String orderId, String deliveryStaffId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        assertOrderActive(order);

        // Never silently assign somebody else when the requested staff member doesn't exist
        DeliveryStaff staff = deliveryStaffRepository.findById(deliveryStaffId)
                .orElseThrow(() -> ResourceNotFoundException.of("DeliveryStaff", deliveryStaffId));

        Delivery delivery = findOrCreateByOrder(order);
        delivery.setDeliveryStaff(staff);

        if (isBeforeTransit(delivery.getStatus())) {
            delivery.setStatus("In Transit");
            order.setStatus("In Transit");
            orderRepository.save(order);
        }
        return toDeliveryDTO(deliveryRepository.save(delivery));
    }


    // ASSIGN ORDER TO DELIVERY STAFF (FROM ORDER MANAGEMENT STEP 3)
    public DeliveryDTO assignOrderStaff(String orderId, String staffName, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        assertOrderActive(order);

        Delivery delivery = findOrCreateByOrder(order);

        if (staffName != null && !staffName.isBlank() && !"Delivery Staff".equalsIgnoreCase(staffName.trim())) {
            DeliveryStaff matched = findStaffByNameOrId(staffName);
            if (matched == null) {
                throw ResourceNotFoundException.of("DeliveryStaff", staffName);
            }
            delivery.setDeliveryStaff(matched);
        }

        String finalStatus = normalizeStatus(status);
        if (finalStatus == null) finalStatus = "In Transit";
        delivery.setStatus(finalStatus);
        if (delivery.getDeliveryDate() == null) {
            delivery.setDeliveryDate(LocalDate.now().plusDays(3));
        }

        order.setStatus(finalStatus);
        orderRepository.save(order);

        return toDeliveryDTO(deliveryRepository.save(delivery));
    }


    // ASSIGN BY STAFF NAME (used from ManageDelivery admin panel)
    public DeliveryDTO assignByStaffName(String deliveryId, String staffName) {
        Delivery delivery = resolveDelivery(deliveryId);

        if (delivery.getOrder() != null) {
            assertOrderActive(delivery.getOrder());
        }

        DeliveryStaff matched = findStaffByNameOrId(staffName);
        if (matched == null) {
            throw ResourceNotFoundException.of("DeliveryStaff", staffName);
        }
        delivery.setDeliveryStaff(matched);

        // Re-assigning must NOT push an "Out for Delivery"/"Delivered" delivery back to "In Transit"
        if (isBeforeTransit(delivery.getStatus())) {
            delivery.setStatus("In Transit");
            if (delivery.getOrder() != null) {
                delivery.getOrder().setStatus("In Transit");
                orderRepository.save(delivery.getOrder());
            }
        }

        return toDeliveryDTO(deliveryRepository.save(delivery));
    }


    // STATUS NORMALIZATION
    private String normalizeStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String clean = raw.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        switch (clean) {
            case "PENDING": return "Pending";
            case "ORDER_CONFIRMED":
            case "CONFIRMED": return "Order Confirmed";
            case "PICKED_AND_PACKED":
            case "PICKED_&_PACKED":
            case "PICKED_PACKED":
            case "PROCESSING": return "Picked & Packed";
            case "IN_TRANSIT":
            case "TRANSIT":
            case "SHIPPED":
            case "DISPATCHED": return "In Transit";
            case "OUT_FOR_DELIVERY":
            case "OUT_DELIVERY": return "Out for Delivery";
            case "DELIVERED": return "Delivered";
            case "CANCELLED":
            case "CANCELED": return "Cancelled";
            case "FAILED": return "Failed";
            case "RETURNED": return "Returned";
            default: return raw.trim();
        }
    }

    // GET DELIVERY BY ID
    @Transactional(readOnly = true)
    public DeliveryDTO getDelivery(String id) {
        Optional<Delivery> result = deliveryRepository.findById(id);
        if (result.isEmpty()) {
            throw ResourceNotFoundException.of("Delivery", id);
        }
        return toDeliveryDTO(result.get());
    }

    // =========================================================
    // GET DELIVERY BY ORDER
    // =========================================================
    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryByOrder(String orderId) {
        Optional<Delivery> result = deliveryRepository.findByOrder_OrderId(orderId);
        if (result.isPresent()) {
            return toDeliveryDTO(result.get());
        }
        Optional<Order> orderResult = orderRepository.findById(orderId);
        if (orderResult.isPresent()) {
            return createPendingDeliveryDTOFromOrder(orderResult.get());
        }
        throw new ResourceNotFoundException("Delivery not found for order: " + orderId);
    }


    // GET DELIVERIES BY STAFF
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesByStaff(String deliveryStaffId) {
        List<Delivery> deliveries = deliveryRepository.findByDeliveryStaff_UserId(deliveryStaffId);
        List<DeliveryDTO> list = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            list.add(toDeliveryDTO(delivery));
        }
        return list;
    }


    // UPDATE EXPECTED DATE
    public DeliveryDTO updateExpectedDate(String id, String expectedDate) {
        Delivery delivery = resolveDelivery(id);

        if (delivery.getOrder() != null) {
            assertOrderActive(delivery.getOrder());
        }

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(expectedDate);
        } catch (Exception e) {
            throw new BusinessRuleException("Invalid date format. Use YYYY-MM-DD, e.g. 2026-10-10");
        }

        if (parsedDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Expected date cannot be in the past.");
        }

        delivery.setDeliveryDate(parsedDate);
        return toDeliveryDTO(deliveryRepository.save(delivery));
    }


    // UPDATE STATUS
    public DeliveryDTO updateStatus(String id, String status) {
        Delivery delivery = resolveDelivery(id);

        String cleanStatus = normalizeStatus(status);
        if (cleanStatus == null) cleanStatus = "Pending";

        Order order = delivery.getOrder();
        if (order != null) {
            String current = order.getStatus() == null ? "" : order.getStatus();
            if ("Cancelled".equalsIgnoreCase(current) && !"Cancelled".equals(cleanStatus)) {
                throw new BusinessRuleException("Order " + order.getOrderId() + " is cancelled; its delivery cannot be changed");
            }
            if ("Delivered".equalsIgnoreCase(current) && !"Delivered".equals(cleanStatus)) {
                throw new BusinessRuleException("Order " + order.getOrderId() + " is already delivered");
            }
        }

        delivery.setStatus(cleanStatus);

        if (order != null) {
            // Failed / Returned only exist on the delivery side, so the order keeps its status.
            switch (cleanStatus) {
                case "Delivered":
                    order.setStatus("Delivered");
                    if (order.getPayment() != null) {
                        order.getPayment().setStatus("COMPLETED");
                    }
                    break;
                case "Out for Delivery":
                case "In Transit":
                case "Picked & Packed":
                case "Order Confirmed":
                case "Cancelled":
                    order.setStatus(cleanStatus);
                    break;
                default:
                    break;
            }
            orderRepository.save(order);
        }

        return toDeliveryDTO(deliveryRepository.save(delivery));
    }


    // DELETE DELIVERY
    public void deleteDelivery(String id) {
        if (!deliveryRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Delivery", id);
        }
        deliveryRepository.deleteById(id);
    }

    // CONVERT ENTITY TO DTO
    private DeliveryDTO toDeliveryDTO(Delivery delivery) {
        DeliveryDTO dto = new DeliveryDTO();
        dto.setDeliveryId(delivery.getDeliveryId());
        dto.setStatus(delivery.getStatus());

        // deliveryDate on the entity = the scheduled/expected delivery date
        dto.setDeliveryDate(delivery.getDeliveryDate());
        dto.setExpectedDate(delivery.getDeliveryDate());

        // deliveredDate is only meaningful once the order is actually delivered
        if ("Delivered".equalsIgnoreCase(delivery.getStatus())) {
            dto.setDeliveredDate(delivery.getDeliveryDate());
        }

        if (delivery.getOrder() != null) {
            Order order = delivery.getOrder();
            dto.setOrderId(order.getOrderId());
            dto.setAmount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);

            // assignedDate = when the order was placed / assigned
            dto.setAssignedDate(order.getOrderDate());

            if (order.getPayment() != null) {
                dto.setPaymentMethod(order.getPayment().getMethod());
                dto.setPaymentStatus(order.getPayment().getStatus());
            }

            if (order.getCustomer() != null) {
                dto.setCustomerId(order.getCustomer().getUserId());
                dto.setCustomerName(order.getCustomer().getName());
                dto.setCustomerEmail(order.getCustomer().getEmail());
                dto.setCustomerPhone(order.getCustomer().getPhone());
                dto.setCustomerImage(order.getCustomer().getUserImage());

                if (order.getCustomer().getAddress() != null) {
                    dto.setAddress(order.getCustomer().getAddress().getStreet());
                    dto.setCity(order.getCustomer().getAddress().getCity());
                    dto.setPostalCode(order.getCustomer().getAddress().getPostalCode());
                }
            }
        }

        if (delivery.getDeliveryStaff() != null) {
            dto.setDeliveryStaffId(delivery.getDeliveryStaff().getUserId());
            dto.setDeliveryStaffName(delivery.getDeliveryStaff().getName());
        }

        return dto;
    }

    private DeliveryDTO createPendingDeliveryDTOFromOrder(Order order) {
        DeliveryDTO dto = new DeliveryDTO();
        dto.setDeliveryId("DEL-" + order.getOrderId());
        dto.setOrderId(order.getOrderId());
        // Mirror the order's real status
        String orderStatus = normalizeStatus(order.getStatus());
        dto.setStatus(orderStatus != null ? orderStatus : "Pending");

        // assignedDate = when the order was placed
        dto.setAssignedDate(order.getOrderDate());

        // expectedDate = order date + 5 days (default SLA for pending orders)
        if (order.getOrderDate() != null) {
            dto.setExpectedDate(order.getOrderDate().plusDays(5));
        }

        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getUserId());
            dto.setCustomerName(order.getCustomer().getName() != null ? order.getCustomer().getName() : order.getCustomer().getUserId());
            dto.setCustomerEmail(order.getCustomer().getEmail());
            dto.setCustomerPhone(order.getCustomer().getPhone());
            dto.setCustomerImage(order.getCustomer().getUserImage());

            if (order.getCustomer().getAddress() != null) {
                dto.setAddress(order.getCustomer().getAddress().getStreet());
                dto.setCity(order.getCustomer().getAddress().getCity());
                dto.setPostalCode(order.getCustomer().getAddress().getPostalCode());
            }
        }

        if (order.getPayment() != null) {
            dto.setAmount(order.getPayment().getAmount());
            dto.setPaymentMethod(order.getPayment().getMethod());
            dto.setPaymentStatus(order.getPayment().getStatus());
        } else {
            dto.setAmount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            dto.setPaymentMethod("Cash on Delivery");
            dto.setPaymentStatus("Pending");
        }

        dto.setDeliveryStaffName("Delivery Staff");
        return dto;
    }
}