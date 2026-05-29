package com.empik.coupons.service;

import static com.empik.coupons.enumerate.CouponRejectionReason.COUPON_USAGE_LIMIT_REACHED;
import static com.empik.coupons.enumerate.CouponRejectionReason.USER_ALREADY_USED_COUPON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.empik.coupons.enumerate.CouponRejectionReason;
import com.empik.coupons.exception.CouponException;
import com.empik.coupons.integration.geolocalisation.GeolocalisationResolver;
import com.empik.coupons.model.entity.CouponEntity;
import com.empik.coupons.model.request.ApplyCouponRequest;
import com.empik.coupons.model.request.CreateCouponRequest;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.repository.CouponUsageRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CouponServiceConcurrencyTests {

  private static final String LOCALHOST_IP = "127.0.0.1";
  private static final String POLAND = "PL";

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
  void shouldAllowOnlyMaxNumberOfConcurrentRedemptions() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var coupon = couponService.createCoupon(new CreateCouponRequest("WIOSNA", 10, POLAND));

    var result = applyConcurrently(100, userId -> new ApplyCouponRequest("wiosna", userId));

    assertThat(result.successCount()).isEqualTo(10);
    assertThat(result.rejections()).filteredOn(COUPON_USAGE_LIMIT_REACHED::equals).hasSize(90);
    assertThat(result.unexpectedFailures()).isEmpty();

    assertThat(currentUsagesOf(coupon.id())).isEqualTo(10);
    assertThat(couponUsageRepository.findAll()).hasSize(10);
  }

  @Test
  void shouldAllowOnlyOneConcurrentRedemptionPerUserForTheSameCoupon() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn(POLAND);

    var coupon = couponService.createCoupon(new CreateCouponRequest("USERONCE", 100, POLAND));

    var result = applyConcurrently(50, ignored -> new ApplyCouponRequest("USERONCE", 999L));

    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.rejections()).filteredOn(USER_ALREADY_USED_COUPON::equals).hasSize(49);
    assertThat(result.unexpectedFailures()).isEmpty();

    assertThat(currentUsagesOf(coupon.id())).isEqualTo(1);

    assertThat(couponUsageRepository.findAll())
        .singleElement()
        .satisfies(
            usage -> {
              assertThat(usage.getCoupon().getId()).isEqualTo(coupon.id());
              assertThat(usage.getUserId()).isEqualTo(999L);
            });
  }

  private ConcurrentApplyResult applyConcurrently(
      int requestsCount, LongFunction<ApplyCouponRequest> requestFactory)
      throws InterruptedException {
    var start = new CountDownLatch(1);
    var successCount = new AtomicInteger();
    var rejections = Collections.synchronizedList(new ArrayList<CouponRejectionReason>());
    var unexpectedFailures = Collections.synchronizedList(new ArrayList<Throwable>());

    var threads =
        LongStream.range(0, requestsCount)
            .mapToObj(
                userId ->
                    new Thread(
                        () -> {
                          await(start);

                          try {
                            couponService.applyCoupon(requestFactory.apply(userId), LOCALHOST_IP);
                            successCount.incrementAndGet();
                          } catch (CouponException exception) {
                            rejections.add(exception.reason());
                          } catch (Throwable throwable) {
                            unexpectedFailures.add(throwable);
                          }
                        }))
            .toList();

    threads.forEach(Thread::start);
    start.countDown();

    for (Thread thread : threads) {
      thread.join();
    }

    return new ConcurrentApplyResult(
        successCount.get(), List.copyOf(rejections), List.copyOf(unexpectedFailures));
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }

  private int currentUsagesOf(Long couponId) {
    return couponRepository.findById(couponId).map(CouponEntity::getCurrentUsages).orElseThrow();
  }

  private record ConcurrentApplyResult(
      int successCount,
      List<CouponRejectionReason> rejections,
      List<Throwable> unexpectedFailures) {}
}
