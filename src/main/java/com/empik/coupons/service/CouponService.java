package com.empik.coupons.service;

import com.empik.coupons.model.request.ApplyCouponRequest;
import com.empik.coupons.model.request.CreateCouponRequest;
import com.empik.coupons.model.response.ApplyCouponResponse;
import com.empik.coupons.model.response.CouponUsageResponse;
import com.empik.coupons.model.response.CreateCouponResponse;
import java.util.List;

public interface CouponService {

  List<CreateCouponResponse> getAllCoupons();

  List<CouponUsageResponse> getAllCouponUsages();

  CreateCouponResponse createCoupon(CreateCouponRequest createCouponRequest);

  ApplyCouponResponse applyCoupon(ApplyCouponRequest applyCouponRequest, String ipAddress);
}
