package com.empik.coupons.model.response;

import java.time.Instant;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateCouponResponse(
    Long id,
    String code,
    Instant createdAt,
    int maxUsages,
    int currentUsages,
    String countryCode) {}
