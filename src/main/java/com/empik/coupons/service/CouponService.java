package com.empik.coupons.service;

import static com.empik.coupons.utils.CouponNormalizer.*;

import com.empik.coupons.enumerate.CouponRejectionReason;
import com.empik.coupons.exception.CouponException;
import com.empik.coupons.integration.geolocalisation.GeolocalisationResolver;
import com.empik.coupons.mapper.CouponMapper;
import com.empik.coupons.mapper.CouponUsageMapper;
import com.empik.coupons.model.entity.CouponEntity;
import com.empik.coupons.model.entity.CouponUsageEntity;
import com.empik.coupons.model.request.ApplyCouponRequest;
import com.empik.coupons.model.request.CreateCouponRequest;
import com.empik.coupons.model.response.ApplyCouponResponse;
import com.empik.coupons.model.response.CouponUsageResponse;
import com.empik.coupons.model.response.CreateCouponResponse;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.repository.CouponUsageRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

  private final String SUCCESS_MESSAGE = "Coupon applied successfully";

  private final CouponRepository couponRepository;
  private final CouponUsageRepository couponUsageRepository;
  private final GeolocalisationResolver geolocalisationResolver;
  private final CouponMapper couponMapper;
  private final CouponUsageMapper couponUsageMapper;

  @Transactional(readOnly = true)
  public List<CreateCouponResponse> getAllCoupons() {
    List<CouponEntity> coupons = couponRepository.findAll();

    return coupons.stream().map(couponMapper::toCreateCouponResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<CouponUsageResponse> getAllCouponUsages() {
    List<CouponUsageEntity> coupons = couponUsageRepository.findAll();

    return coupons.stream().map(couponUsageMapper::toCouponUsageResponse).toList();
  }

  @Transactional
  public CreateCouponResponse createCoupon(CreateCouponRequest createCouponRequest) {
    validateIfCouponExist(createCouponRequest.code());

    CouponEntity coupon = couponMapper.toEntity(createCouponRequest);
    CouponEntity savedCoupon = saveCoupon(coupon);

    return couponMapper.toCreateCouponResponse(savedCoupon);
  }

  @Transactional
  public ApplyCouponResponse applyCoupon(ApplyCouponRequest applyCouponRequest, String ipAddress) {
    CouponEntity coupon = findCouponByCode(applyCouponRequest.code());
    String requestCountryCode = geolocalisationResolver.resolveCountryCode(ipAddress);

    validateIfCouponHaBeenUsed(coupon.getId(), applyCouponRequest.userId());
    validateCouponCountry(coupon, requestCountryCode);
    saveCouponUsage(coupon, applyCouponRequest.userId(), ipAddress, requestCountryCode);
    incrementUsageIfLimitNotReached(coupon);

    return ApplyCouponResponse.builder()
        .success(true)
        .code(coupon.getCode())
        .message(SUCCESS_MESSAGE)
        .build();
  }

  private CouponEntity findCouponByCode(String code) {
    String normalizedCode = normalizeCode(code);

    return couponRepository
        .findByCode(normalizedCode)
        .orElseThrow(
            () ->
                new CouponException(
                    CouponRejectionReason.COUPON_NOT_FOUND,
                    CouponRejectionReason.COUPON_NOT_FOUND.getDescription()));
  }

  private void saveCouponUsage(
      CouponEntity coupon, Long userId, String ipAddress, String requestCountryCode) {
    try {
      CouponUsageEntity couponUsageEntity =
          CouponUsageEntity.builder()
              .coupon(coupon)
              .userId(userId)
              .ipAddress(ipAddress)
              .countryCode(requestCountryCode)
              .usedAt(Instant.now())
              .build();

      couponUsageRepository.saveAndFlush(couponUsageEntity);
    } catch (DataIntegrityViolationException ex) {
      throw couponAlreadyUsed();
    }
  }

  private CouponEntity saveCoupon(CouponEntity coupon) {
    try {
      return couponRepository.save(coupon);
    } catch (DataIntegrityViolationException ex) {
      throw couponAlreadyExists();
    }
  }

  private void incrementUsageIfLimitNotReached(CouponEntity coupon) {
    int updatedRows = couponRepository.incrementUsageIfLimitNotReached(coupon.getId());

    if (updatedRows == 0) {
      throw couponUsageLimitReached();
    }
  }

  private void validateIfCouponExist(String code) {
    String normalizedCode = normalizeCode(code);

    if (couponRepository.existsByCode(normalizedCode)) {
      throw couponAlreadyExists();
    }
  }

  private void validateIfCouponHaBeenUsed(Long couponId, Long userId) {
    if (couponUsageRepository.existsByCouponIdAndUserId(couponId, userId)) {
      throw couponAlreadyUsed();
    }
  }

  private void validateCouponCountry(CouponEntity coupon, String requestCountryCode) {
    if (!coupon.getCountryCode().equalsIgnoreCase(requestCountryCode)) {
      throw countryNotAllowed();
    }
  }

  private CouponException couponAlreadyExists() {
    return new CouponException(
        CouponRejectionReason.COUPON_ALREADY_EXISTS,
        CouponRejectionReason.COUPON_ALREADY_EXISTS.getDescription());
  }

  private CouponException countryNotAllowed() {
    return new CouponException(
        CouponRejectionReason.COUNTRY_NOT_ALLOWED,
        CouponRejectionReason.COUNTRY_NOT_ALLOWED.getDescription());
  }

  private CouponException couponUsageLimitReached() {
    return new CouponException(
        CouponRejectionReason.COUPON_USAGE_LIMIT_REACHED,
        CouponRejectionReason.COUPON_USAGE_LIMIT_REACHED.getDescription());
  }

  private CouponException couponAlreadyUsed() {
    return new CouponException(
        CouponRejectionReason.USER_ALREADY_USED_COUPON,
        CouponRejectionReason.USER_ALREADY_USED_COUPON.getDescription());
  }
}
