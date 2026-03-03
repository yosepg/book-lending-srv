package com.demandlane;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.demandlane.TestFixtures.ADMIN_PASS;
import static com.demandlane.TestFixtures.ADMIN_USER;
import static com.demandlane.TestFixtures.USER_PASS;
import static com.demandlane.TestFixtures.USER_USER;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberResourceTest {

    private static final String PATH_MEMBERS = "/api/v1/members";

    private static Long memberId;

    // ── CREATE ──

    @Test
    @Order(1)
    void createMember_asAdmin_returns201() {
        memberId = given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"name":"Alice Wonderland","email":"mem-test-alice@example.com"}
                    """)
                .when()
                .post(PATH_MEMBERS)
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("data.id", notNullValue())
                .body("data.name", is("Alice Wonderland"))
                .body("data.email", is("mem-test-alice@example.com"))
                .body("timestamp", notNullValue())
                .extract().jsonPath().getLong("data.id");
    }

    @Test
    @Order(2)
    void createMember_asUser_returns403() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .contentType("application/json")
                .body("""
                    {"name":"Forbidden User","email":"mem-test-forbidden@example.com"}
                    """)
                .when()
                .post(PATH_MEMBERS)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(3)
    void createMember_duplicateEmail_returns409() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"name":"Alice Copy","email":"mem-test-alice@example.com"}
                    """)
                .when()
                .post(PATH_MEMBERS)
                .then()
                .statusCode(409)
                .body("errorCode", is("DUPLICATE_ENTITY"))
                .body("errorMessage", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(4)
    void createMember_blankName_returns400() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"name":"","email":"mem-test-valid@example.com"}
                    """)
                .when()
                .post(PATH_MEMBERS)
                .then()
                .statusCode(400)
                .body("errorCode", is("VALIDATION_ERROR"));
    }

    @Test
    @Order(5)
    void createMember_invalidEmail_returns400() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"name":"Bad Email","email":"not-an-email"}
                    """)
                .when()
                .post(PATH_MEMBERS)
                .then()
                .statusCode(400)
                .body("errorCode", is("VALIDATION_ERROR"));
    }

    // ── READ ──

    @Test
    @Order(10)
    void listMembers_asAdmin_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_MEMBERS)
                .then()
                .statusCode(200)
                .body("data.size()", greaterThanOrEqualTo(1))
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(11)
    void listMembers_asUser_returns403() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .when()
                .get(PATH_MEMBERS)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(12)
    void getMember_byId_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_MEMBERS + "/" + memberId)
                .then()
                .statusCode(200)
                .body("data.id", is(memberId.intValue()))
                .body("data.name", is("Alice Wonderland"))
                .body("data.email", is("mem-test-alice@example.com"));
    }

    @Test
    @Order(13)
    void getMember_notFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_MEMBERS + "/99999")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(14)
    void unauthenticated_returns401() {
        given()
                .when()
                .get(PATH_MEMBERS)
                .then()
                .statusCode(401);
    }

    // ── UPDATE ──

    @Test
    @Order(20)
    void updateMember_asAdmin_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"name":"Alice Updated","email":"mem-test-alice@example.com"}
                    """)
                .when()
                .put(PATH_MEMBERS + "/" + memberId)
                .then()
                .statusCode(200)
                .body("data.name", is("Alice Updated"));
    }

    @Test
    @Order(21)
    void updateMember_notFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"name":"Ghost","email":"mem-test-ghost@example.com"}
                    """)
                .when()
                .put(PATH_MEMBERS + "/99999")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    // ── DELETE ──

    @Test
    @Order(30)
    void deleteMember_notFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .delete(PATH_MEMBERS + "/99999")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(31)
    void deleteMember_asAdmin_returns204() {
        Long disposable = TestFixtures.createMember("Disposable", "mem-test-dispose@example.com");

        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .delete(PATH_MEMBERS + "/" + disposable)
                .then()
                .statusCode(204);

        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_MEMBERS + "/" + disposable)
                .then()
                .statusCode(404);
    }
}
