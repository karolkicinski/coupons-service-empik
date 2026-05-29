package com.empik.coupons.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.model.entity.CouponEntity;
import com.empik.coupons.model.entity.CouponUsageEntity;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CouponUsageMapperTests {

  private final CouponUsageMapper couponUsageMapper = Mappers.getMapper(CouponUsageMapper.class);

  @Test
  void shouldMapCouponUsageEntityToCouponUsageResponse() {
    var usedAt = Instant.parse("2026-05-29T10:15:30Z");

    var coupon =
        CouponEntity.builder()
            .id(1L)
            .code("WIOSNA")
            .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
            .maxUsages(10)
            .currentUsages(3)
            .countryCode("PL")
            .build();

    var usage =
        CouponUsageEntity.builder()
            .id(100L)
            .coupon(coupon)
            .userId(200L)
            .usedAt(usedAt)
            .ipAddress("127.0.0.1")
            .countryCode("PL")
            .build();

    var response = couponUsageMapper.toCouponUsageResponse(usage);

    assertThat(response.id()).isEqualTo(100L);
    assertThat(response.couponId()).isEqualTo(1L);
    assertThat(response.couponCode()).isEqualTo("WIOSNA");
    assertThat(response.userId()).isEqualTo(200L);
    assertThat(response.usedAt()).isEqualTo(usedAt);
    assertThat(response.ipAddress()).isEqualTo("127.0.0.1");
    assertThat(response.countryCode()).isEqualTo("PL");
  }

  @Test
  void shouldReturnNullWhenMappingNullUsageToResponse() {
    var response = couponUsageMapper.toCouponUsageResponse(null);

    assertThat(response).isNull();
  }

  @Test
  void shouldMapUsageWithNullCoupon() {
    var usedAt = Instant.parse("2026-05-29T10:15:30Z");

    var usage =
        CouponUsageEntity.builder()
            .id(100L)
            .coupon(null)
            .userId(200L)
            .usedAt(usedAt)
            .ipAddress("127.0.0.1")
            .countryCode("PL")
            .build();

    var response = couponUsageMapper.toCouponUsageResponse(usage);

    assertThat(response.id()).isEqualTo(100L);
    assertThat(response.couponId()).isNull();
    assertThat(response.couponCode()).isNull();
    assertThat(response.userId()).isEqualTo(200L);
    assertThat(response.usedAt()).isEqualTo(usedAt);
    assertThat(response.ipAddress()).isEqualTo("127.0.0.1");
    assertThat(response.countryCode()).isEqualTo("PL");
  }
}
