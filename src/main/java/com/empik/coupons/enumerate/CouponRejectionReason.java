package com.empik.coupons.enumerate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponRejectionReason {
  COUPON_NOT_FOUND("Coupon does not exist"),
  COUPON_ALREADY_EXISTS("Coupon with given code already exists"),
  COUPON_USAGE_LIMIT_REACHED("Coupon usage limit has been reached"),
  COUNTRY_NOT_ALLOWED("Coupon cannot be used from this country"),
  USER_ALREADY_USED_COUPON("User has already used this coupon");

  private final String description;
}
