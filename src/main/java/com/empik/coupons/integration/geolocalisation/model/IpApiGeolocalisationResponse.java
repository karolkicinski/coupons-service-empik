package com.empik.coupons.integration.geolocalisation.model;

public record IpApiGeolocalisationResponse(String status, String message, String countryCode) {
  private static final String SUCCESS_FLAG = "success";

  public boolean isSuccess() {
    return SUCCESS_FLAG.equalsIgnoreCase(status);
  }
}
