package com.empik.coupons.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Table(
    name = "coupons",
    uniqueConstraints = {@UniqueConstraint(name = "uq_coupon_code", columnNames = "code")})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "max_usages", nullable = false)
  private int maxUsages;

  @Column(name = "current_usages", nullable = false)
  private int currentUsages;

  @Column(name = "country_code", nullable = false, length = 2)
  private String countryCode;
}
