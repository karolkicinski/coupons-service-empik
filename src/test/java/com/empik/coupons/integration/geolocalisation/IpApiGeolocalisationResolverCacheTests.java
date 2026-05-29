package com.empik.coupons.integration.geolocalisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.empik.coupons.exception.CountryResolutionException;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpApiGeolocalisationResolverCacheTests {

  private static final String IP_ADDRESS = "127.0.0.1";
  private static final String COUNTRY_CODE = "PL";

  @Mock private GeolocalisationResolver delegate;

  @Mock private IMap<String, String> ipCountryCache;

  @Test
  void shouldReturnCountryCodeFromCacheWhenPresent() {
    var resolver = new IpApiGeolocalisationResolverCache(delegate, ipCountryCache);

    when(ipCountryCache.get(IP_ADDRESS)).thenReturn(COUNTRY_CODE);

    var countryCode = resolver.resolveCountryCode(IP_ADDRESS);

    assertThat(countryCode).isEqualTo(COUNTRY_CODE);

    verify(ipCountryCache).get(IP_ADDRESS);
    verify(delegate, never()).resolveCountryCode(IP_ADDRESS);
    verify(ipCountryCache, never()).put(IP_ADDRESS, COUNTRY_CODE);
  }

  @Test
  void shouldResolveCountryCodeAndStoreItInCacheWhenMissing() {
    var resolver = new IpApiGeolocalisationResolverCache(delegate, ipCountryCache);

    when(ipCountryCache.get(IP_ADDRESS)).thenReturn(null);
    when(delegate.resolveCountryCode(IP_ADDRESS)).thenReturn(COUNTRY_CODE);

    var countryCode = resolver.resolveCountryCode(IP_ADDRESS);

    assertThat(countryCode).isEqualTo(COUNTRY_CODE);

    verify(ipCountryCache).get(IP_ADDRESS);
    verify(delegate).resolveCountryCode(IP_ADDRESS);
    verify(ipCountryCache).put(IP_ADDRESS, COUNTRY_CODE);
  }

  @Test
  void shouldNotStoreAnythingInCacheWhenDelegateFails() {
    var resolver = new IpApiGeolocalisationResolverCache(delegate, ipCountryCache);

    when(ipCountryCache.get(IP_ADDRESS)).thenReturn(null);
    when(delegate.resolveCountryCode(IP_ADDRESS))
        .thenThrow(new CountryResolutionException("Cannot resolve country"));

    assertThatThrownBy(() -> resolver.resolveCountryCode(IP_ADDRESS))
        .isInstanceOf(CountryResolutionException.class)
        .hasMessage("Cannot resolve country");

    verify(ipCountryCache).get(IP_ADDRESS);
    verify(delegate).resolveCountryCode(IP_ADDRESS);
    verify(ipCountryCache, never()).put(IP_ADDRESS, COUNTRY_CODE);
  }
}
