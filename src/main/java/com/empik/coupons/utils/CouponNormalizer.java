package com.empik.coupons.utils;

import java.util.Locale;

public class CouponNormalizer {

  public static String normalizeCode(String code) {
    return normalize(code, "Coupon code cannot be null");
  }

  public static String normalizeCountry(String country) {
    return normalize(country, "Country code cannot be null");
  }

  private static String normalize(String value, String nullMessage) {
    if (value == null) {
      throw new IllegalArgumentException(nullMessage);
    }

    return value.trim().toUpperCase(Locale.ROOT);
  }
}
