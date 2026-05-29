package com.empik.coupons.model.response;

import java.time.Instant;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CouponUsageResponse(
    Long id,
    Long couponId,
    String couponCode,
    Long userId,
    Instant usedAt,
    String ipAddress,
    String countryCode) {}
