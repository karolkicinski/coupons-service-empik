package com.empik.coupons.controller;

import com.empik.coupons.enumerate.CouponRejectionReason;
import com.empik.coupons.exception.CountryResolutionException;
import com.empik.coupons.exception.CouponException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(CouponException.class)
  ResponseEntity<ErrorResponse> handleCouponException(CouponException ex) {
    return ResponseEntity.status(statusFor(ex.reason()))
        .body(new ErrorResponse(Instant.now(), ex.reason().name(), ex.getMessage()));
  }

  @ExceptionHandler(CountryResolutionException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  ErrorResponse handleCountryResolutionException(CountryResolutionException ex) {
    return new ErrorResponse(Instant.now(), "COUNTRY_RESOLUTION_FAILED", ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, String> details = new LinkedHashMap<>();

    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }

    return new ErrorResponse(
        Instant.now(), "VALIDATION_ERROR", "Request validation failed", details);
  }

  private HttpStatus statusFor(CouponRejectionReason reason) {
    return switch (reason) {
      case COUPON_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case COUNTRY_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
      case COUPON_ALREADY_EXISTS, COUPON_USAGE_LIMIT_REACHED, USER_ALREADY_USED_COUPON ->
          HttpStatus.CONFLICT;
    };
  }

  record ErrorResponse(
      Instant timestamp, String code, String message, Map<String, String> details) {

    ErrorResponse(Instant timestamp, String code, String message) {
      this(timestamp, code, message, null);
    }
  }
}
