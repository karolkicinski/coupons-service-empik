package com.empik.coupons.integration.geolocalisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.empik.coupons.exception.CountryResolutionException;
import com.empik.coupons.integration.geolocalisation.feign.IpApiGeolocalisationFeignClient;
import com.empik.coupons.integration.geolocalisation.impl.IpApiGeolocalisationResolver;
import com.empik.coupons.integration.geolocalisation.model.IpApiGeolocalisationResponse;
import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpApiGeolocalisationResolverTests {

  private static final String IP_ADDRESS = "127.0.0.1";
  private static final String FIELDS = "status,message,countryCode";

  @Mock private IpApiGeolocalisationFeignClient ipApiClient;

  @Test
  void shouldResolveCountryCode() {
    var resolver = new IpApiGeolocalisationResolver(ipApiClient);

    when(ipApiClient.resolve(IP_ADDRESS, FIELDS))
        .thenReturn(new IpApiGeolocalisationResponse("success", null, "pl"));

    var countryCode = resolver.resolveCountryCode(IP_ADDRESS);

    assertThat(countryCode).isEqualTo("PL");
    verify(ipApiClient).resolve(IP_ADDRESS, FIELDS);
  }

  @Test
  void shouldThrowExceptionWhenProviderReturnsFailureStatus() {
    var resolver = new IpApiGeolocalisationResolver(ipApiClient);

    when(ipApiClient.resolve(IP_ADDRESS, FIELDS))
        .thenReturn(new IpApiGeolocalisationResponse("fail", "invalid query", null));

    assertThatThrownBy(() -> resolver.resolveCountryCode(IP_ADDRESS))
        .isInstanceOf(CountryResolutionException.class)
        .hasMessage("Cannot resolve country for IP: " + IP_ADDRESS);
  }

  @Test
  void shouldThrowExceptionWhenProviderReturnsNullResponse() {
    var resolver = new IpApiGeolocalisationResolver(ipApiClient);

    when(ipApiClient.resolve(IP_ADDRESS, FIELDS)).thenReturn(null);

    assertThatThrownBy(() -> resolver.resolveCountryCode(IP_ADDRESS))
        .isInstanceOf(CountryResolutionException.class)
        .hasMessage("Cannot resolve country for IP: " + IP_ADDRESS);
  }

  @Test
  void shouldThrowExceptionWhenProviderReturnsSuccessWithoutCountryCode() {
    var resolver = new IpApiGeolocalisationResolver(ipApiClient);

    when(ipApiClient.resolve(IP_ADDRESS, FIELDS))
        .thenReturn(new IpApiGeolocalisationResponse("success", null, null));

    assertThatThrownBy(() -> resolver.resolveCountryCode(IP_ADDRESS))
        .isInstanceOf(CountryResolutionException.class)
        .hasMessage("Cannot resolve country for IP: " + IP_ADDRESS);
  }

  @Test
  void shouldThrowExceptionWhenProviderIsUnavailable() {
    var resolver = new IpApiGeolocalisationResolver(ipApiClient);

    when(ipApiClient.resolve(IP_ADDRESS, FIELDS)).thenThrow(feignException());

    assertThatThrownBy(() -> resolver.resolveCountryCode(IP_ADDRESS))
        .isInstanceOf(CountryResolutionException.class)
        .hasMessage("IP geolocation provider is unavailable");
  }

  private FeignException feignException() {
    var request =
        Request.create(
            Request.HttpMethod.GET,
            "/json/" + IP_ADDRESS,
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            null);

    return new FeignException.ServiceUnavailable("Service unavailable", request, null, Map.of());
  }
}
