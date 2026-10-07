package com.sliit.ecommerce.Entitys;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for the "Contains" relationship between ShoppingCart and Product.
 */
public class CartItemId implements Serializable {

    private String cart;
    private String product;

    public CartItemId() {
    }

    public CartItemId(String cart, String product) {
        this.cart = cart;
        this.product = product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItemId)) return false;
        CartItemId that = (CartItemId) o;
        return Objects.equals(cart, that.cart) && Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cart, product);
    }
}
