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
class BookResourceTest {

    private static final String PATH_BOOKS = "/api/v1/books";

    private static Long bookId;

    // ── CREATE ──

    @Test
    @Order(1)
    void createBook_asAdmin_returns201() {
        bookId = given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"Clean Code","author":"Robert C. Martin","isbn":"978-BK-001","totalCopies":3}
                    """)
                .when()
                .post(PATH_BOOKS)
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("data.id", notNullValue())
                .body("data.title", is("Clean Code"))
                .body("data.author", is("Robert C. Martin"))
                .body("data.isbn", is("978-BK-001"))
                .body("data.availableCopies", is(3))
                .body("data.totalCopies", is(3))
                .body("timestamp", notNullValue())
                .extract().jsonPath().getLong("data.id");
    }

    @Test
    @Order(2)
    void createBook_asUser_returns403() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"Forbidden","author":"Test","isbn":"978-BK-FORBID","totalCopies":1}
                    """)
                .when()
                .post(PATH_BOOKS)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(3)
    void createBook_duplicateIsbn_returns409() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"Duplicate","author":"Dup","isbn":"978-BK-001","totalCopies":1}
                    """)
                .when()
                .post(PATH_BOOKS)
                .then()
                .statusCode(409)
                .body("errorCode", is("DUPLICATE_ENTITY"))
                .body("errorMessage", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(4)
    void createBook_blankTitle_returns400() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"","author":"Author","isbn":"978-BK-VALID","totalCopies":1}
                    """)
                .when()
                .post(PATH_BOOKS)
                .then()
                .statusCode(400)
                .body("errorCode", is("VALIDATION_ERROR"));
    }

    @Test
    @Order(5)
    void createBook_zeroCopies_returns400() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"Valid Title","author":"Author","isbn":"978-BK-VALID2","totalCopies":0}
                    """)
                .when()
                .post(PATH_BOOKS)
                .then()
                .statusCode(400)
                .body("errorCode", is("VALIDATION_ERROR"));
    }

    // ── READ ──

    @Test
    @Order(10)
    void listBooks_asAdmin_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_BOOKS)
                .then()
                .statusCode(200)
                .body("data.size()", greaterThanOrEqualTo(1))
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(11)
    void listBooks_asUser_returns200() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .when()
                .get(PATH_BOOKS)
                .then()
                .statusCode(200)
                .body("data.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(12)
    void getBook_byId_returns200() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .when()
                .get(PATH_BOOKS + "/" + bookId)
                .then()
                .statusCode(200)
                .body("data.id", is(bookId.intValue()))
                .body("data.title", is("Clean Code"));
    }

    @Test
    @Order(13)
    void getBook_notFound_returns404() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .when()
                .get(PATH_BOOKS + "/99999")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(14)
    void unauthenticated_returns401() {
        given()
                .when()
                .get(PATH_BOOKS)
                .then()
                .statusCode(401);
    }

    // ── UPDATE ──

    @Test
    @Order(20)
    void updateBook_asAdmin_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"Clean Code 2nd Ed","author":"Robert C. Martin","isbn":"978-BK-001","totalCopies":5}
                    """)
                .when()
                .put(PATH_BOOKS + "/" + bookId)
                .then()
                .statusCode(200)
                .body("data.title", is("Clean Code 2nd Ed"))
                .body("data.totalCopies", is(5))
                .body("data.availableCopies", is(5));
    }

    @Test
    @Order(21)
    void updateBook_notFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"Ghost","author":"Ghost","isbn":"978-BK-GHOST","totalCopies":1}
                    """)
                .when()
                .put(PATH_BOOKS + "/99999")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(22)
    void updateBook_asUser_returns403() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .contentType("application/json")
                .body("""
                    {"title":"Hacked","author":"Hack","isbn":"978-BK-001","totalCopies":1}
                    """)
                .when()
                .put(PATH_BOOKS + "/" + bookId)
                .then()
                .statusCode(403);
    }

    // ── DELETE ──

    @Test
    @Order(30)
    void deleteBook_asUser_returns403() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .when()
                .delete(PATH_BOOKS + "/" + bookId)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(31)
    void deleteBook_notFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .delete(PATH_BOOKS + "/99999")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(32)
    void deleteBook_asAdmin_returns204() {
        Long disposable = TestFixtures.createBook("Disposable", "Author", "978-BK-DEL", 1);

        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .delete(PATH_BOOKS + "/" + disposable)
                .then()
                .statusCode(204);

        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_BOOKS + "/" + disposable)
                .then()
                .statusCode(404);
    }
}
