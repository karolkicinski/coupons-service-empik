package com.empik.coupons.repository;

import com.empik.coupons.model.entity.CouponEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

  Optional<CouponEntity> findByCode(String code);

  boolean existsByCode(String code);

  @Modifying
  @Query(
      """
        UPDATE CouponEntity c
           SET c.currentUsages = c.currentUsages + 1
         WHERE c.id = :couponId
           AND c.currentUsages < c.maxUsages
    """)
  int incrementUsageIfLimitNotReached(@Param("couponId") Long couponId);
}
