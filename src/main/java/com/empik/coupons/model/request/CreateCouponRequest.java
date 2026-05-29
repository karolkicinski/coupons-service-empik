package com.empik.coupons.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateCouponRequest(
    @NotBlank @Size(max = 128) String code,
    @Min(1) int maxUsages,
    @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode) {}
