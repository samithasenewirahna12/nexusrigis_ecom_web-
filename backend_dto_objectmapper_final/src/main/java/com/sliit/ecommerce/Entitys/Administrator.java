package com.sliit.ecommerce.Entitys;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "administrator")
@PrimaryKeyJoinColumn(name = "user_id")
public class Administrator extends User {

    @Column(name = "access_level", length = 30)
    private String accessLevel;

    // Manages (1:N) -> Product
    @OneToMany(mappedBy = "managedByAdmin")
    private List<Product> managedProducts = new ArrayList<>();

    // Manages (1:N) -> Category
    @OneToMany(mappedBy = "managedByAdmin")
    private List<Category> managedCategories = new ArrayList<>();

    public Administrator() {
        super();
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public List<Product> getManagedProducts() {
        return managedProducts;
    }

    public void setManagedProducts(List<Product> managedProducts) {
        this.managedProducts = managedProducts;
    }

    public List<Category> getManagedCategories() {
        return managedCategories;
    }

    public void setManagedCategories(List<Category> managedCategories) {
        this.managedCategories = managedCategories;
    }
}
