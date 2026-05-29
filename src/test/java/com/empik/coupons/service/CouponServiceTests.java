package com.empik.coupons.service;

import static com.empik.coupons.enumerate.CouponRejectionReason.COUNTRY_NOT_ALLOWED;
import static com.empik.coupons.enumerate.CouponRejectionReason.COUPON_ALREADY_EXISTS;
import static com.empik.coupons.enumerate.CouponRejectionReason.COUPON_NOT_FOUND;
import static com.empik.coupons.enumerate.CouponRejectionReason.COUPON_USAGE_LIMIT_REACHED;
import static com.empik.coupons.enumerate.CouponRejectionReason.USER_ALREADY_USED_COUPON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.empik.coupons.exception.CouponException;
import com.empik.coupons.integration.geolocalisation.GeolocalisationResolver;
import com.empik.coupons.model.entity.CouponEntity;
import com.empik.coupons.model.request.ApplyCouponRequest;
import com.empik.coupons.model.request.CreateCouponRequest;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.repository.CouponUsageRepository;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CouponServiceTests {

  private static final String LOCALHOST_IP = "127.0.0.1";
  private static final String POLAND = "PL";
  private static final String GERMANY = "DE";

  @Autowired private CouponService couponService;

  @Autowired private CouponRepository couponRepository;

  @Autowired private CouponUsageRepository couponUsageRepository;

  @MockitoBean private GeolocalisationResolver geolocalisationResolver;

  @AfterEach
  void cleanUp() {
    couponUsageRepository.deleteAll();
    couponRepository.deleteAll();
  }

  @Test
  void shouldCreateCouponWithNormalizedCodeAndCountryAndInitialUsageCounter() {
    var response = couponService.createCoupon(new CreateCouponRequest("  wiosna2026  ", 10, "pl"));

    assertThat(response.id()).isNotNull();
    assertThat(response.code()).isEqualTo("WIOSNA2026");
    assertThat(response.maxUsages()).isEqualTo(10);
    assertThat(response.currentUsages()).isZero();
    assertThat(response.countryCode()).isEqualTo(POLAND);
    assertThat(response.createdAt()).isNotNull();

    assertThat(couponRepository.findAll())
        .singleElement()
        .satisfies(
            coupon -> {
              assertThat(coupon.getCode()).isEqualTo("WIOSNA2026");
              assertThat(coupon.getMaxUsages()).isEqualTo(10);
              assertThat(coupon.getCurrentUsages()).isZero();
              assertThat(coupon.getCountryCode()).isEqualTo(POLAND);
              assertThat(coupon.getCreatedAt()).isNotNull();
            });
  }

  @Test
  void shouldRejectCreatingCouponWhenNormalizedCodeAlreadyExists() {
    couponService.createCoupon(new CreateCouponRequest("WIOSNA", 10, POLAND));

    assertThatThrownBy(
            () -> couponService.createCoupon(new CreateCouponRequest("  wiosna  ", 5, POLAND)))
        .isInstanceOf(CouponException.class)
        .satisfies(exception -> assertReason(exception, COUPON_ALREADY_EXISTS));

    assertThat(couponRepository.findAll()).hasSize(1);
  }

  @Test
  void shouldApplyCouponSuccessfully() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var coupon = couponService.createCoupon(new CreateCouponRequest("WIOSNA", 3, POLAND));

    var response = couponService.applyCoupon(new ApplyCouponRequest("WIOSNA", 100L), LOCALHOST_IP);

    assertThat(response.success()).isTrue();
    assertThat(response.code()).isEqualTo("WIOSNA");
    assertThat(response.message()).isEqualTo("Coupon applied successfully");

    assertThat(couponRepository.findById(coupon.id()))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(1);

    assertThat(couponUsageRepository.findAll())
        .singleElement()
        .satisfies(
            usage -> {
              assertThat(usage.getCoupon().getId()).isEqualTo(coupon.id());
              assertThat(usage.getUserId()).isEqualTo(100L);
              assertThat(usage.getIpAddress()).isEqualTo(LOCALHOST_IP);
              assertThat(usage.getCountryCode()).isEqualTo(POLAND);
              assertThat(usage.getUsedAt()).isNotNull();
            });

    verify(geolocalisationResolver).resolveCountryCode(LOCALHOST_IP);
  }

  @Test
  void shouldApplyCouponCaseInsensitivelyAndWithTrimmedCode() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    couponService.createCoupon(new CreateCouponRequest("WIOSNA", 3, POLAND));

    var response =
        couponService.applyCoupon(new ApplyCouponRequest("  wiosna  ", 101L), LOCALHOST_IP);

    assertThat(response.success()).isTrue();
    assertThat(response.code()).isEqualTo("WIOSNA");

    assertThat(couponRepository.findByCode("WIOSNA"))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(1);

    assertThat(couponUsageRepository.findAll()).hasSize(1);
  }

  @Test
  void shouldRejectApplyingNotExistingCoupon() {
    assertThatThrownBy(
            () -> couponService.applyCoupon(new ApplyCouponRequest("BRAK", 100L), LOCALHOST_IP))
        .isInstanceOf(CouponException.class)
        .satisfies(exception -> assertReason(exception, COUPON_NOT_FOUND));

    assertThat(couponRepository.findAll()).isEmpty();
    assertThat(couponUsageRepository.findAll()).isEmpty();
  }

  @Test
  void shouldRejectCouponWhenCountryIsNotAllowed() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(GERMANY);

    var coupon = couponService.createCoupon(new CreateCouponRequest("WIOSNA", 3, POLAND));

    assertThatThrownBy(
            () -> couponService.applyCoupon(new ApplyCouponRequest("WIOSNA", 100L), LOCALHOST_IP))
        .isInstanceOf(CouponException.class)
        .satisfies(exception -> assertReason(exception, COUNTRY_NOT_ALLOWED));

    assertThat(couponRepository.findById(coupon.id()))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(0);

    assertThat(couponUsageRepository.findAll()).isEmpty();
  }

  @Test
  void shouldRejectCouponWhenUsageLimitIsReached() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var coupon = couponService.createCoupon(new CreateCouponRequest("LIMIT", 1, POLAND));

    couponService.applyCoupon(new ApplyCouponRequest("LIMIT", 100L), LOCALHOST_IP);

    assertThatThrownBy(
            () -> couponService.applyCoupon(new ApplyCouponRequest("LIMIT", 101L), LOCALHOST_IP))
        .isInstanceOf(CouponException.class)
        .satisfies(exception -> assertReason(exception, COUPON_USAGE_LIMIT_REACHED));

    assertThat(couponRepository.findById(coupon.id()))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(1);

    assertThat(couponUsageRepository.findAll())
        .singleElement()
        .satisfies(
            usage -> {
              assertThat(usage.getUserId()).isEqualTo(100L);
              assertThat(usage.getCoupon().getId()).isEqualTo(coupon.id());
            });
  }

  @Test
  void shouldRejectSecondUsageByTheSameUserForTheSameCoupon() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var coupon = couponService.createCoupon(new CreateCouponRequest("JEDENRAZ", 10, POLAND));

    couponService.applyCoupon(new ApplyCouponRequest("JEDENRAZ", 100L), LOCALHOST_IP);

    assertThatThrownBy(
            () -> couponService.applyCoupon(new ApplyCouponRequest("JEDENRAZ", 100L), LOCALHOST_IP))
        .isInstanceOf(CouponException.class)
        .satisfies(exception -> assertReason(exception, USER_ALREADY_USED_COUPON));

    assertThat(couponRepository.findById(coupon.id()))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(1);

    assertThat(couponUsageRepository.findAll())
        .singleElement()
        .satisfies(
            usage -> {
              assertThat(usage.getUserId()).isEqualTo(100L);
              assertThat(usage.getCoupon().getId()).isEqualTo(coupon.id());
            });
  }

  @Test
  void shouldAllowSameUserToUseDifferentCoupons() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var firstCoupon = couponService.createCoupon(new CreateCouponRequest("KOD1", 10, POLAND));
    var secondCoupon = couponService.createCoupon(new CreateCouponRequest("KOD2", 10, POLAND));

    couponService.applyCoupon(new ApplyCouponRequest("KOD1", 100L), LOCALHOST_IP);
    couponService.applyCoupon(new ApplyCouponRequest("KOD2", 100L), LOCALHOST_IP);

    assertThat(couponRepository.findById(firstCoupon.id()))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(1);

    assertThat(couponRepository.findById(secondCoupon.id()))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(1);

    assertThat(couponUsageRepository.findAll()).hasSize(2);
  }

  @Test
  void shouldReturnAllCouponsMappedToResponses() {
    couponService.createCoupon(new CreateCouponRequest("ZIMA", 1, POLAND));
    couponService.createCoupon(new CreateCouponRequest("LATO", 2, GERMANY));

    var coupons =
        couponService.getAllCoupons().stream()
            .sorted(Comparator.comparing(response -> response.code()))
            .toList();

    assertThat(coupons).hasSize(2);

    assertThat(coupons.get(0).code()).isEqualTo("LATO");
    assertThat(coupons.get(0).maxUsages()).isEqualTo(2);
    assertThat(coupons.get(0).currentUsages()).isZero();
    assertThat(coupons.get(0).countryCode()).isEqualTo(GERMANY);

    assertThat(coupons.get(1).code()).isEqualTo("ZIMA");
    assertThat(coupons.get(1).maxUsages()).isEqualTo(1);
    assertThat(coupons.get(1).currentUsages()).isZero();
    assertThat(coupons.get(1).countryCode()).isEqualTo(POLAND);
  }

  @Test
  void shouldReturnAllCouponUsagesMappedToResponses() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var coupon = couponService.createCoupon(new CreateCouponRequest("USAGES", 10, POLAND));

    couponService.applyCoupon(new ApplyCouponRequest("USAGES", 100L), LOCALHOST_IP);
    couponService.applyCoupon(new ApplyCouponRequest("USAGES", 101L), LOCALHOST_IP);

    var usages =
        couponService.getAllCouponUsages().stream()
            .sorted(Comparator.comparing(response -> response.userId()))
            .toList();

    assertThat(usages).hasSize(2);

    assertThat(usages.get(0).id()).isNotNull();
    assertThat(usages.get(0).couponId()).isEqualTo(coupon.id());
    assertThat(usages.get(0).couponCode()).isEqualTo("USAGES");
    assertThat(usages.get(0).userId()).isEqualTo(100L);
    assertThat(usages.get(0).ipAddress()).isEqualTo(LOCALHOST_IP);
    assertThat(usages.get(0).countryCode()).isEqualTo(POLAND);
    assertThat(usages.get(0).usedAt()).isNotNull();

    assertThat(usages.get(1).id()).isNotNull();
    assertThat(usages.get(1).couponId()).isEqualTo(coupon.id());
    assertThat(usages.get(1).couponCode()).isEqualTo("USAGES");
    assertThat(usages.get(1).userId()).isEqualTo(101L);
    assertThat(usages.get(1).ipAddress()).isEqualTo(LOCALHOST_IP);
    assertThat(usages.get(1).countryCode()).isEqualTo(POLAND);
    assertThat(usages.get(1).usedAt()).isNotNull();
  }

  @Test
  void shouldNotPersistUsageWhenLimitIsAlreadyReached() {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var coupon = couponService.createCoupon(new CreateCouponRequest("ROLLBACK", 1, POLAND));

    couponService.applyCoupon(new ApplyCouponRequest("ROLLBACK", 100L), LOCALHOST_IP);

    assertThatThrownBy(
            () -> couponService.applyCoupon(new ApplyCouponRequest("ROLLBACK", 101L), LOCALHOST_IP))
        .isInstanceOf(CouponException.class)
        .satisfies(exception -> assertReason(exception, COUPON_USAGE_LIMIT_REACHED));

    assertThat(couponRepository.findById(coupon.id()))
        .isPresent()
        .get()
        .extracting(CouponEntity::getCurrentUsages)
        .isEqualTo(1);

    assertThat(couponUsageRepository.findAll())
        .singleElement()
        .satisfies(usage -> assertThat(usage.getUserId()).isEqualTo(100L));
  }

  private static void assertReason(Throwable throwable, Object expectedReason) {
    assertThat(throwable)
        .isInstanceOf(CouponException.class)
        .extracting(exception -> ((CouponException) exception).reason())
        .isEqualTo(expectedReason);
  }
}
