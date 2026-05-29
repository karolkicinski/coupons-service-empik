package com.empik.coupons.mapper;

import com.empik.coupons.model.entity.CouponUsageEntity;
import com.empik.coupons.model.response.CouponUsageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponUsageMapper {

  @Mapping(target = "couponId", source = "coupon.id")
  @Mapping(target = "couponCode", source = "coupon.code")
  CouponUsageResponse toCouponUsageResponse(CouponUsageEntity couponUsage);
}
