package com.sliit.ecommerce.Entitys;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for the weak entity ORDER ITEM: (OrderID, LineNo).
 * LineNo is the partial key; OrderID is inherited from the identifying
 * "Contains" relationship with ORDER.
 */
public class OrderItemId implements Serializable {

    private String order;
    private Integer lineNo;

    public OrderItemId() {
    }

    public OrderItemId(String order, Integer lineNo) {
        this.order = order;
        this.lineNo = lineNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemId)) return false;
        OrderItemId that = (OrderItemId) o;
        return Objects.equals(order, that.order) && Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, lineNo);
    }
}
