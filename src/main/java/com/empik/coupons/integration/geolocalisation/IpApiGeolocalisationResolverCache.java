package com.empik.coupons.integration.geolocalisation;

import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class IpApiGeolocalisationResolverCache implements GeolocalisationResolver {

  private final GeolocalisationResolver delegate;
  private final IMap<String, String> ipCountryCache;

  @Override
  public String resolveCountryCode(String ipAddress) {
    String cached = ipCountryCache.get(ipAddress);

    if (cached != null) {
      return cached;
    }

    String countryCode = delegate.resolveCountryCode(ipAddress);
    ipCountryCache.put(ipAddress, countryCode);

    return countryCode;
  }
}
