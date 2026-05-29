package com.empik.coupons.integration.geolocalisation;

import com.empik.coupons.exception.CountryResolutionException;
import com.empik.coupons.integration.geolocalisation.feign.IpApiGeolocalisationFeignClient;
import com.empik.coupons.integration.geolocalisation.model.IpApiGeolocalisationResponse;
import feign.FeignException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IpApiGeolocalisationResolver implements GeolocalisationResolver {

  private static final String FIELDS = "status,message,countryCode";

  private final IpApiGeolocalisationFeignClient ipApiClient;

  @Override
  public String resolveCountryCode(String ipAddress) {
    try {
      IpApiGeolocalisationResponse response = ipApiClient.resolve(ipAddress, FIELDS);

      if (response == null || !response.isSuccess() || response.countryCode() == null) {
        throw new CountryResolutionException("Cannot resolve country for IP: " + ipAddress);
      }

      return response.countryCode().toUpperCase(Locale.ROOT);
    } catch (FeignException ex) {
      throw new CountryResolutionException("IP geolocation provider is unavailable");
    }
  }
}
