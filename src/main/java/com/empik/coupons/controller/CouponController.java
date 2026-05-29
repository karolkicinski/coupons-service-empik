package com.empik.coupons.controller;

import static com.empik.coupons.utils.HttpUtils.extractClientIp;

import com.empik.coupons.model.request.ApplyCouponRequest;
import com.empik.coupons.model.request.CreateCouponRequest;
import com.empik.coupons.model.response.ApplyCouponResponse;
import com.empik.coupons.model.response.CouponUsageResponse;
import com.empik.coupons.model.response.CreateCouponResponse;
import com.empik.coupons.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Operations for creating, applying and inspecting coupons")
public class CouponController {
  private final CouponService couponService;

  @Operation(
      summary = "Create coupon",
      description = "Creates a new coupon with usage limit and country restriction.")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateCouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
    log.info(
        "Received create coupon request: code={}, countryCode={}",
        request.code(),
        request.countryCode());

    CreateCouponResponse response = couponService.createCoupon(request);

    log.info("Coupon created successfully: id={}, code={}", response.id(), response.code());
    return response;
  }

  @Operation(
      summary = "Apply coupon",
      description =
          "Applies coupon for a user. The request IP address is used to resolve country and validate coupon availability.")
  @PostMapping("/apply")
  public ApplyCouponResponse applyCoupon(
      @Valid @RequestBody ApplyCouponRequest request, HttpServletRequest servletRequest) {
    String ipAddress = extractClientIp(servletRequest);

    log.info(
        "Received apply coupon request: code={}, userId={}, ipAddress={}",
        request.code(),
        request.userId(),
        ipAddress);

    ApplyCouponResponse response = couponService.applyCoupon(request, ipAddress);

    log.info("Coupon applied successfully: code={}, userId={}", response.code(), request.userId());
    return response;
  }

  // for testing purposes
  @Operation(
      summary = "Get all coupons",
      description = "Returns all coupons. Endpoint intended mainly for testing purposes.")
  @GetMapping
  public List<CreateCouponResponse> getAllCoupons() {
    log.info("Received get all coupons request");

    List<CreateCouponResponse> response = couponService.getAllCoupons();

    log.info("Returned coupons: count={}", response.size());
    return response;
  }

  @Operation(
      summary = "Get all coupon usages",
      description =
          "Returns all coupon usage records. Endpoint intended mainly for testing purposes.")
  @GetMapping("/usages")
  public List<CouponUsageResponse> getAllCouponsUsages() {
    log.info("Received get all coupons usages request");

    List<CouponUsageResponse> response = couponService.getAllCouponUsages();

    log.info("Returned usages: count={}", response.size());
    return response;
  }
}
