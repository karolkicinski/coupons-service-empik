package com.empik.coupons.integration.geolocalisation.feign;

import com.empik.coupons.integration.geolocalisation.model.IpApiGeolocalisationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ip-geolocalisation-service", url = "${integrations.geo.provider-url}")
public interface IpApiGeolocalisationFeignClient {
  @GetMapping("/json/{ip}")
  IpApiGeolocalisationResponse resolve(
      @PathVariable("ip") String ip, @RequestParam("fields") String fields);
}
