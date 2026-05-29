package com.empik.coupons.integration.geolocalisation.feign;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class IpApiGeolocalisationFeignClientContractTests {

  private static final String IP_ADDRESS = "127.0.0.1";
  private static final String FIELDS = "status,message,countryCode";

  private static WireMockServer wireMockServer;

  @Autowired private IpApiGeolocalisationFeignClient feignClient;

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    wireMockServer.stop();
  }

  @BeforeEach
  void resetWireMock() {
    wireMockServer.resetAll();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("integrations.geo.provider-url", wireMockServer::baseUrl);
  }

  @Test
  void shouldCallIpApiEndpointAndMapResponse() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/json/" + IP_ADDRESS))
            .withQueryParam("fields", equalTo(FIELDS))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                                                {
                                                  "status": "success",
                                                  "message": null,
                                                  "countryCode": "PL"
                                                }
                                                """)));

    var response = feignClient.resolve(IP_ADDRESS, FIELDS);

    assertThat(response.status()).isEqualTo("success");
    assertThat(response.message()).isNull();
    assertThat(response.countryCode()).isEqualTo("PL");
    assertThat(response.isSuccess()).isTrue();

    wireMockServer.verify(
        getRequestedFor(urlPathEqualTo("/json/" + IP_ADDRESS))
            .withQueryParam("fields", equalTo(FIELDS)));
  }
}
