package com.empik.coupons.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpUtilsTests {

  @Mock private HttpServletRequest request;

  @Test
  void shouldExtractIpFromXForwardedForHeader() {
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");

    var ipAddress = HttpUtils.extractClientIp(request);

    assertThat(ipAddress).isEqualTo("203.0.113.10");
  }

  @Test
  void shouldExtractFirstIpFromXForwardedForHeaderWhenHeaderContainsMultipleIps() {
    when(request.getHeader("X-Forwarded-For"))
        .thenReturn("203.0.113.10, 198.51.100.20, 192.0.2.30");

    var ipAddress = HttpUtils.extractClientIp(request);

    assertThat(ipAddress).isEqualTo("203.0.113.10");
  }

  @Test
  void shouldTrimIpExtractedFromXForwardedForHeader() {
    when(request.getHeader("X-Forwarded-For")).thenReturn("   203.0.113.10   , 198.51.100.20");

    var ipAddress = HttpUtils.extractClientIp(request);

    assertThat(ipAddress).isEqualTo("203.0.113.10");
  }

  @Test
  void shouldUseRemoteAddressWhenXForwardedForHeaderIsMissing() {
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    var ipAddress = HttpUtils.extractClientIp(request);

    assertThat(ipAddress).isEqualTo("127.0.0.1");
  }

  @Test
  void shouldUseRemoteAddressWhenXForwardedForHeaderIsBlank() {
    when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    var ipAddress = HttpUtils.extractClientIp(request);

    assertThat(ipAddress).isEqualTo("127.0.0.1");
  }
}
