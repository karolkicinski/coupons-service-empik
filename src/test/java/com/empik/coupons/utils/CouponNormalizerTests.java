package com.empik.coupons.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CouponNormalizerTests {

  @Test
  void shouldNormalizeCouponCode() {
    var normalizedCode = CouponNormalizer.normalizeCode("  wiosna2026  ");

    assertThat(normalizedCode).isEqualTo("WIOSNA2026");
  }

  @Test
  void shouldNormalizeCountryCode() {
    var normalizedCountry = CouponNormalizer.normalizeCountry("  pl  ");

    assertThat(normalizedCountry).isEqualTo("PL");
  }

  @Test
  void shouldKeepAlreadyNormalizedCouponCodeUnchanged() {
    var normalizedCode = CouponNormalizer.normalizeCode("BLACKFRIDAY");

    assertThat(normalizedCode).isEqualTo("BLACKFRIDAY");
  }

  @Test
  void shouldKeepAlreadyNormalizedCountryCodeUnchanged() {
    var normalizedCountry = CouponNormalizer.normalizeCountry("PL");

    assertThat(normalizedCountry).isEqualTo("PL");
  }

  @Test
  void shouldReturnEmptyStringWhenCouponCodeContainsOnlyWhitespaces() {
    var normalizedCode = CouponNormalizer.normalizeCode("   ");

    assertThat(normalizedCode).isEmpty();
  }

  @Test
  void shouldReturnEmptyStringWhenCountryCodeContainsOnlyWhitespaces() {
    var normalizedCountry = CouponNormalizer.normalizeCountry("   ");

    assertThat(normalizedCountry).isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenCouponCodeIsNull() {
    assertThatThrownBy(() -> CouponNormalizer.normalizeCode(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Coupon code cannot be null");
  }

  @Test
  void shouldThrowExceptionWhenCountryCodeIsNull() {
    assertThatThrownBy(() -> CouponNormalizer.normalizeCountry(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Country code cannot be null");
  }
}
