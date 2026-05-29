package com.empik.coupons.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

  @Bean
  Config hazelcastInstanceConfig() {
    Config config = new Config();
    config.setClusterName("coupon-service");

    config.addMapConfig(new MapConfig("ip-country-cache").setTimeToLiveSeconds(3600));

    return config;
  }

  @Bean
  IMap<String, String> ipCountryCache(HazelcastInstance hazelcastInstance) {
    return hazelcastInstance.getMap("ip-country-cache");
  }
}
