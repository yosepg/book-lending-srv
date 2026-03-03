package com.demandlane;

import static io.restassured.RestAssured.given;

public final class TestFixtures {

    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASS = "admin123";
    public static final String USER_USER = "alice";
    public static final String USER_PASS = "alice123";
    private static final String PATH_LOANS = "/api/v1/loans";
    private static final String PATH_BORROW = "api/v1/loans/borrow";
    private static final String PATH_BOOKS = "/api/v1/books";
    private static final String PATH_MEMBERS = "/api/v1/members";

    private TestFixtures() {}

    public static Long createBook(String title, String author, String isbn, int totalCopies) {
        return given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"title":"%s","author":"%s","isbn":"%s","totalCopies":%d}
                    """, title, author, isbn, totalCopies))
                .when()
                .post(PATH_BOOKS)
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("data.id");
    }

    public static Long createMember(String name, String email) {
        return given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"name":"%s","email":"%s"}
                    """, name, email))
                .when()
                .post(PATH_MEMBERS)
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("data.id");
    }

    public static Long borrowBook(Long bookId, Long memberId) {
        return given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":%d}
                    """, bookId, memberId))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("data.id");
    }

    public static void returnLoan(Long loanId) {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .post(PATH_LOANS + "/" + loanId + "/return")
                .then()
                .statusCode(200);
    }
}
