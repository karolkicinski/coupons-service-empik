package com.empik.coupons.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Table(
    name = "coupon_usages",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_coupon_user_usage",
          columnNames = {"coupon_id", "user_id"})
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id", nullable = false)
  private CouponEntity coupon;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "used_at", nullable = false)
  private Instant usedAt;

  @Column(name = "ip_address", nullable = false)
  private String ipAddress;

  @Column(name = "country_code", nullable = false, length = 2)
  private String countryCode;
}
