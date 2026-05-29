package com.empik.coupons.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.empik.coupons.model.entity.CouponEntity;
import com.empik.coupons.model.request.CreateCouponRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CouponMapperTests {

  private final CouponMapper couponMapper = Mappers.getMapper(CouponMapper.class);

  @Test
  void shouldMapCreateCouponRequestToEntity() {
    var request = new CreateCouponRequest("  wiosna2026  ", 10, "pl");

    var beforeMapping = Instant.now();
    var entity = couponMapper.toEntity(request);
    var afterMapping = Instant.now();

    assertThat(entity.getId()).isNull();
    assertThat(entity.getCode()).isEqualTo("WIOSNA2026");
    assertThat(entity.getMaxUsages()).isEqualTo(10);
    assertThat(entity.getCurrentUsages()).isZero();
    assertThat(entity.getCountryCode()).isEqualTo("PL");
    assertThat(entity.getCreatedAt()).isBetween(beforeMapping, afterMapping);
  }

  @Test
  void shouldMapCouponEntityToCreateCouponResponse() {
    var createdAt = Instant.parse("2026-05-29T10:15:30Z");

    var entity =
        CouponEntity.builder()
            .id(1L)
            .code("WIOSNA")
            .createdAt(createdAt)
            .maxUsages(10)
            .currentUsages(3)
            .countryCode("PL")
            .build();

    var response = couponMapper.toCreateCouponResponse(entity);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.code()).isEqualTo("WIOSNA");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.maxUsages()).isEqualTo(10);
    assertThat(response.currentUsages()).isEqualTo(3);
    assertThat(response.countryCode()).isEqualTo("PL");
  }

  @Test
  void shouldReturnNullWhenMappingNullEntityToResponse() {
    var response = couponMapper.toCreateCouponResponse(null);

    assertThat(response).isNull();
  }

  @Test
  void shouldThrowExceptionWhenRequestCodeIsNull() {
    var request = new CreateCouponRequest(null, 10, "PL");

    assertThatThrownBy(() -> couponMapper.toEntity(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Coupon code cannot be null");
  }

  @Test
  void shouldThrowExceptionWhenRequestCountryIsNull() {
    var request = new CreateCouponRequest("WIOSNA", 10, null);

    assertThatThrownBy(() -> couponMapper.toEntity(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Country code cannot be null");
  }
}
