package com.empik.coupons.mapper;

import com.empik.coupons.model.entity.CouponEntity;
import com.empik.coupons.model.request.CreateCouponRequest;
import com.empik.coupons.model.response.CreateCouponResponse;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    imports = {Instant.class})
public interface CouponMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(
      target = "code",
      expression = "java(com.empik.coupons.utils.CouponNormalizer.normalizeCode(request.code()))")
  @Mapping(target = "createdAt", expression = "java(Instant.now())")
  @Mapping(target = "currentUsages", constant = "0")
  @Mapping(
      target = "countryCode",
      expression =
          "java(com.empik.coupons.utils.CouponNormalizer.normalizeCountry(request.countryCode()))")
  CouponEntity toEntity(CreateCouponRequest request);

  CreateCouponResponse toCreateCouponResponse(CouponEntity coupon);
}
