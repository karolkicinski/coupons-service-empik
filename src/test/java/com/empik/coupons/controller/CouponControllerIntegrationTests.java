package com.empik.coupons.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.empik.coupons.exception.CountryResolutionException;
import com.empik.coupons.integration.geolocalisation.GeolocalisationResolver;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.repository.CouponUsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CouponControllerIntegrationTests {

  private static final String LOCALHOST_IP = "127.0.0.1";
  private static final String FORWARDED_IP = "203.0.113.10";

  @Autowired private MockMvc mockMvc;

  @Autowired private CouponRepository couponRepository;

  @Autowired private CouponUsageRepository couponUsageRepository;

  @MockitoBean private GeolocalisationResolver geolocalisationResolver;

  @AfterEach
  void cleanUp() {
    couponUsageRepository.deleteAll();
    couponRepository.deleteAll();
  }

  @Test
  void shouldCreateCoupon() throws Exception {
    mockMvc
        .perform(
            post("/api/coupons")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "  wiosna2026  ",
                                          "max_usages": 10,
                                          "country_code": "pl"
                                        }
                                        """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.code").value("WIOSNA2026"))
        .andExpect(jsonPath("$.max_usages").value(10))
        .andExpect(jsonPath("$.current_usages").value(0))
        .andExpect(jsonPath("$.country_code").value("PL"))
        .andExpect(jsonPath("$.created_at").exists());

    mockMvc
        .perform(get("/api/coupons"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].code").value("WIOSNA2026"))
        .andExpect(jsonPath("$[0].max_usages").value(10))
        .andExpect(jsonPath("$[0].current_usages").value(0))
        .andExpect(jsonPath("$[0].country_code").value("PL"));
  }

  @Test
  void shouldRejectCreatingCouponWhenNormalizedCodeAlreadyExists() throws Exception {
    createCoupon("WIOSNA", 10, "PL");

    mockMvc
        .perform(
            post("/api/coupons")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                    {
                                      "code": "  wiosna  ",
                                      "max_usages": 5,
                                      "country_code": "pl"
                                    }
                                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COUPON_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.message").value("Coupon with given code already exists"))
        .andExpect(jsonPath("$.timestamp").exists());

    mockMvc
        .perform(get("/api/coupons"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].code").value("WIOSNA"))
        .andExpect(jsonPath("$[0].max_usages").value(10))
        .andExpect(jsonPath("$[0].country_code").value("PL"));
  }

  @Test
  void shouldReturnBadGatewayWhenCountryResolutionFails() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP))
        .thenThrow(new CountryResolutionException("IP geolocation provider is unavailable"));

    createCoupon("WIOSNA", 10, "PL");

    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                    {
                                      "code": "WIOSNA",
                                      "user_id": 100
                                    }
                                    """))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("COUNTRY_RESOLUTION_FAILED"))
        .andExpect(jsonPath("$.message").value("IP geolocation provider is unavailable"))
        .andExpect(jsonPath("$.timestamp").exists());

    mockMvc
        .perform(get("/api/coupons/usages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void shouldApplyCoupon() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn("PL");

    createCoupon("WIOSNA", 10, "PL");

    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "wiosna",
                                          "user_id": 100
                                        }
                                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("WIOSNA"))
        .andExpect(jsonPath("$.message").value("Coupon applied successfully"));

    mockMvc
        .perform(get("/api/coupons/usages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].coupon_code").value("WIOSNA"))
        .andExpect(jsonPath("$[0].user_id").value(100))
        .andExpect(jsonPath("$[0].ip_address").value(LOCALHOST_IP))
        .andExpect(jsonPath("$[0].country_code").value("PL"))
        .andExpect(jsonPath("$[0].used_at").exists());
  }

  @Test
  void shouldApplyCouponUsingIpFromXForwardedForHeader() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(FORWARDED_IP)).thenReturn("PL");

    createCoupon("WIOSNA", 10, "PL");

    mockMvc
        .perform(
            post("/api/coupons/apply")
                .header("X-Forwarded-For", FORWARDED_IP + ", 198.51.100.20")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "WIOSNA",
                                          "user_id": 100
                                        }
                                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("WIOSNA"));

    mockMvc
        .perform(get("/api/coupons/usages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].ip_address").value(FORWARDED_IP));
  }

  @Test
  void shouldRejectApplyingCouponFromNotAllowedCountry() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn("DE");

    createCoupon("WIOSNA", 10, "PL");

    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "WIOSNA",
                                          "user_id": 100
                                        }
                                        """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COUNTRY_NOT_ALLOWED"))
        .andExpect(jsonPath("$.message").value("Coupon cannot be used from this country"))
        .andExpect(jsonPath("$.timestamp").exists());

    mockMvc
        .perform(get("/api/coupons/usages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void shouldRejectApplyingNotExistingCoupon() throws Exception {
    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "UNKNOWN",
                                          "user_id": 100
                                        }
                                        """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Coupon does not exist"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void shouldRejectApplyingCouponWhenUsageLimitIsReached() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn("PL");

    createCoupon("LIMIT", 1, "PL");

    applyCoupon("LIMIT", 100L);

    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "LIMIT",
                                          "user_id": 101
                                        }
                                        """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COUPON_USAGE_LIMIT_REACHED"))
        .andExpect(jsonPath("$.message").value("Coupon usage limit has been reached"))
        .andExpect(jsonPath("$.timestamp").exists());

    mockMvc
        .perform(get("/api/coupons/usages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void shouldRejectApplyingCouponTwiceBySameUser() throws Exception {
    when(geolocalisationResolver.resolveCountryCode(LOCALHOST_IP)).thenReturn("PL");

    createCoupon("ONCE", 10, "PL");

    applyCoupon("ONCE", 100L);

    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "ONCE",
                                          "user_id": 100
                                        }
                                        """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_ALREADY_USED_COUPON"))
        .andExpect(jsonPath("$.message").value("User has already used this coupon"))
        .andExpect(jsonPath("$.timestamp").exists());

    mockMvc
        .perform(get("/api/coupons/usages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void shouldRejectCreatingCouponWithInvalidRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/coupons")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "",
                                          "max_usages": 0,
                                          "country_code": "POL"
                                        }
                                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.details.code").exists())
        .andExpect(jsonPath("$.details.maxUsages").exists())
        .andExpect(jsonPath("$.details.countryCode").exists());
  }

  @Test
  void shouldRejectApplyingCouponWithInvalidRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "",
                                          "user_id": -1
                                        }
                                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.details.code").exists())
        .andExpect(jsonPath("$.details.userId").exists());
  }

  private void createCoupon(String code, int maxUsages, String countryCode) throws Exception {
    mockMvc
        .perform(
            post("/api/coupons")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "%s",
                                          "max_usages": %d,
                                          "country_code": "%s"
                                        }
                                        """
                        .formatted(code, maxUsages, countryCode)))
        .andExpect(status().isCreated());
  }

  private void applyCoupon(String code, Long userId) throws Exception {
    mockMvc
        .perform(
            post("/api/coupons/apply")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "code": "%s",
                                          "user_id": %d
                                        }
                                        """
                        .formatted(code, userId)))
        .andExpect(status().isOk());
  }
}
