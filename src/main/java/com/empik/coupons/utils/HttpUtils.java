package com.empik.coupons.utils;

import jakarta.servlet.http.HttpServletRequest;

public class HttpUtils {

  public static String extractClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");

    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }

    return request.getRemoteAddr();
  }
}
