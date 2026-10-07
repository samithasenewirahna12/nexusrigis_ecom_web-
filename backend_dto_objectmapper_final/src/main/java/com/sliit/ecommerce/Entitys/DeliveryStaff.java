package com.sliit.ecommerce.Entitys;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_staff")
@PrimaryKeyJoinColumn(name = "user_id")
public class DeliveryStaff extends User {

    @Column(name = "vehicle_no", length = 30)
    private String vehicleNo;

    @Column(name = "license_no", length = 30)
    private String licenseNo;

    // Delivers (1:N) -> Delivery
    @OneToMany(mappedBy = "deliveryStaff", cascade = CascadeType.ALL)
    private List<Delivery> deliveries = new ArrayList<>();

    public DeliveryStaff() {
        super();
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public List<Delivery> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<Delivery> deliveries) {
        this.deliveries = deliveries;
    }
}
