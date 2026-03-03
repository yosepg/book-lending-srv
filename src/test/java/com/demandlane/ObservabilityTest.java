package com.demandlane;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class ObservabilityTest {

    @Test
    void healthCheck_liveness_returns200() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void healthCheck_readiness_returns200() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void healthCheck_combined_returns200() {
        given()
                .when()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void metricsEndpoint_returnsPrometheusFormat() {
        given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("jvm_memory"));
    }

    @Test
    void openApiEndpoint_returnsSpec() {
        given()
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200);
    }
}
