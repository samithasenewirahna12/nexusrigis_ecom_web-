package com.sliit.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Business error that carries the HTTP status to return. */
public class ApiException extends RuntimeException {

  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}