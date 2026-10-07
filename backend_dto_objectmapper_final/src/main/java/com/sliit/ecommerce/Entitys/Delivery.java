package com.sliit.ecommerce.Entitys;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "delivery")
public class Delivery {

    @Id
    @Column(name = "delivery_id")
    private String deliveryId;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "status", length = 30)
    private String status;

    // Fulfils (1:1) <- Order
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // Delivers (N:1) <- DeliveryStaff
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_staff_id", nullable = true)
    private DeliveryStaff deliveryStaff;

    public Delivery() {
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public DeliveryStaff getDeliveryStaff() {
        return deliveryStaff;
    }

    public void setDeliveryStaff(DeliveryStaff deliveryStaff) {
        this.deliveryStaff = deliveryStaff;
    }
}
