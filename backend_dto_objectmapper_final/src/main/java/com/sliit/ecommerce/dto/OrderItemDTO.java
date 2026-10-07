package com.sliit.ecommerce.dto;



import java.math.BigDecimal;

public class OrderItemDTO {

    private Integer lineNo;

    private String productId;

    private String productName;

    private Integer quantity;

    private BigDecimal unitPrice;

    public OrderItemDTO() {}

    public OrderItemDTO(Integer lineNo, String productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.lineNo = lineNo;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

}
