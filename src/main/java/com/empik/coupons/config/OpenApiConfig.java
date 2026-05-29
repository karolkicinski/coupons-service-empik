package com.empik.coupons.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI couponsOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Coupons API")
                .description(
                    """
                                        REST API for managing and applying coupons.

                                        Main features:
                                        - create coupons
                                        - apply coupons by user and IP-based country validation
                                        - inspect coupons and coupon usages for testing purposes
                                        """)
                .version("v1"));
  }
}
