package com.empik.coupons.repository;

import com.empik.coupons.model.entity.CouponUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsageEntity, Long> {

  boolean existsByCouponIdAndUserId(Long couponId, Long userId);
}
