package com.sliit.ecommerce.exception;

/**
 * Thrown for domain/business rule violations that are not simple validation
 * errors, e.g. insufficient stock, duplicate payment for an order, expired coupon.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
