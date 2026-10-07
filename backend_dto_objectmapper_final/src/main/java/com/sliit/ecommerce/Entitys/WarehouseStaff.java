package com.sliit.ecommerce.Entitys;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouse_staff")
@PrimaryKeyJoinColumn(name = "user_id")
public class WarehouseStaff extends User {

    @Column(name = "warehouse_loc", length = 100)
    private String warehouseLoc;

    // UpdatesStock (1:N) -> Product
    @OneToMany(mappedBy = "stockUpdatedBy")
    private List<Product> stockUpdates = new ArrayList<>();

    public WarehouseStaff() {
    }

    public String getWarehouseLoc() {
        return warehouseLoc;
    }

    public void setWarehouseLoc(String warehouseLoc) {
        this.warehouseLoc = warehouseLoc;
    }

    public List<Product> getStockUpdates() {
        return stockUpdates;
    }

    public void setStockUpdates(List<Product> stockUpdates) {
        this.stockUpdates = stockUpdates;
    }
}
