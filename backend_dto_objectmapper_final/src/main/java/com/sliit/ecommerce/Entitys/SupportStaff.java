package com.sliit.ecommerce.Entitys;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_staff")
@PrimaryKeyJoinColumn(name = "user_id")
public class SupportStaff extends User {

    @Column(name = "department", length = 80)
    private String department;

    // HandledBy (1:N) -> Return
    @OneToMany(mappedBy = "handledBy", cascade = CascadeType.ALL)
    private List<ReturnRequest> handledReturns = new ArrayList<>();

    public SupportStaff() {
        super();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<ReturnRequest> getHandledReturns() {
        return handledReturns;
    }

    public void setHandledReturns(List<ReturnRequest> handledReturns) {
        this.handledReturns = handledReturns;
    }
}
