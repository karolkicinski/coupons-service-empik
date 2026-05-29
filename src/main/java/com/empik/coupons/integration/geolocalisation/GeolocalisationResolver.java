package com.empik.coupons.integration.geolocalisation;

public interface GeolocalisationResolver {
  String resolveCountryCode(String ipAddress);
}
