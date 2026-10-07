package com.sliit.ecommerce.Entitys;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "name", nullable = false, length = 150,unique = true)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "discription", columnDefinition = "TEXT")
    private String discription;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty = 0;

    // Multivalued attribute "Images" -> own collection table product_images(product_id, image_url)
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", length = 1500)
    private List<String> images = new ArrayList<>();

    // BelongsTo (N:1) -> Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Manages (N:1) <- Administrator
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Administrator managedByAdmin;

    // UpdatesStock (N:1) <- WarehouseStaff
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_staff_id")
    private WarehouseStaff stockUpdatedBy;

    public Product() {
    }

    public Administrator getManagedByAdmin() {
        return managedByAdmin;
    }

    public void setManagedByAdmin(Administrator managedByAdmin) {
        this.managedByAdmin = managedByAdmin;
    }

    public WarehouseStaff getStockUpdatedBy() {
        return stockUpdatedBy;
    }

    public void setStockUpdatedBy(WarehouseStaff stockUpdatedBy) {
        this.stockUpdatedBy = stockUpdatedBy;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getStockQty() {
        return stockQty;
    }

    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDiscription() {
        return discription;
    }

    public void setDiscription(String discription) {
        this.discription = discription;
    }

    public String getDescription() {
        return discription;
    }

    public void setDescription(String description) {
        this.discription = description;
    }
}
