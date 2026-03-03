package com.demandlane;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanResourceTest {

    private static final String PATH_LOANS = "/api/v1/loans";
    private static final String PATH_BORROW = "api/v1/loans/borrow";
    private static final String PATH_BOOKS = "/api/v1/books";
    private static final String PATH_MEMBERS = "/api/v1/members";

    @Inject
    TestDataHelper testDataHelper;

    private static Long bookId;
    private static Long memberId;
    private static Long loanId;

    // ── SETUP ──

    @Test
    @Order(1)
    void setup_testData() {
        bookId = TestFixtures.createBook("Effective Java", "Joshua Bloch", "978-LN-001", 2);
        memberId = TestFixtures.createMember("Bob Builder", "loan-test-bob@example.com");
    }

    // ── BORROW ──

    @Test
    @Order(10)
    void borrowBook_asAdmin_returns201() {
        loanId = given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":%d}
                    """, bookId, memberId))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("data.id", notNullValue())
                .body("data.bookId", is(bookId.intValue()))
                .body("data.memberId", is(memberId.intValue()))
                .body("data.dueDate", notNullValue())
                .body("data.returnedAt", nullValue())
                .body("timestamp", notNullValue())
                .extract().jsonPath().getLong("data.id");
    }

    @Test
    @Order(11)
    void borrowBook_asUser_returns201() {
        Long bookId2 = TestFixtures.createBook("Refactoring", "Martin Fowler", "978-LN-002", 1);

        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":%d}
                    """, bookId2, memberId))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(201)
                .body("data.id", notNullValue());
    }

    @Test
    @Order(12)
    void borrowBook_bookNotFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":99999,"memberId":%d}
                    """, memberId))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(13)
    void borrowBook_memberNotFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":99999}
                    """, bookId))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(14)
    void borrowBook_noCopies_returns409() {
        Long singleCopyBook = TestFixtures.createBook("Scarce", "Author", "978-LN-SCARCE", 1);
        Long tempMember = TestFixtures.createMember("Temp Borrower", "loan-test-temp@example.com");

        TestFixtures.borrowBook(singleCopyBook, tempMember);

        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":%d}
                    """, singleCopyBook, memberId))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(409)
                .body("errorCode", is("BORROWING_RULE_VIOLATION"))
                .body("errorMessage", notNullValue());
    }

    @Test
    @Order(15)
    void borrowBook_maxLoansReached_returns409() {
        Long freshMember = TestFixtures.createMember("Max Loans Tester", "loan-test-max@example.com");

        for (int i = 0; i < 3; i++) {
            Long b = TestFixtures.createBook("MaxLoanBook" + i, "Author", "978-LN-MAX" + i, 5);
            TestFixtures.borrowBook(b, freshMember);
        }

        Long extraBook = TestFixtures.createBook("OneMore", "Author", "978-LN-EXTRA", 5);
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":%d}
                    """, extraBook, freshMember))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(409)
                .body("errorCode", is("BORROWING_RULE_VIOLATION"))
                .body("errorMessage", notNullValue());
    }

    @Test
    @Order(16)
    void borrowBook_overdueBlocks_returns409() {
        Long overdueMember = TestFixtures.createMember("Overdue Tester", "loan-test-overdue@example.com");
        Long overdueBook = TestFixtures.createBook("Overdue Book", "Author", "978-LN-OVRD", 5);

        TestFixtures.borrowBook(overdueBook, overdueMember);

        testDataHelper.makeLoansOverdue(overdueMember);

        Long anotherBook = TestFixtures.createBook("Another Book", "Author", "978-LN-OVRD2", 5);
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":%d}
                    """, anotherBook, overdueMember))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(409)
                .body("errorCode", is("BORROWING_RULE_VIOLATION"))
                .body("errorMessage", notNullValue());
    }

    @Test
    @Order(17)
    void borrowBook_unauthenticated_returns401() {
        given()
                .contentType("application/json")
                .body(String.format("""
                    {"bookId":%d,"memberId":%d}
                    """, bookId, memberId))
                .when()
                .post(PATH_BORROW)
                .then()
                .statusCode(401);
    }

    // ── GET BY ID ──

    @Test
    @Order(20)
    void getLoan_byId_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_LOANS + "/" + loanId)
                .then()
                .statusCode(200)
                .body("data.id", is(loanId.intValue()))
                .body("data.bookId", is(bookId.intValue()))
                .body("data.memberId", is(memberId.intValue()));
    }

    @Test
    @Order(21)
    void getLoan_notFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_LOANS + "/99999")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    @Test
    @Order(22)
    void getLoan_asUser_returns200() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .when()
                .get(PATH_LOANS + "/" + loanId)
                .then()
                .statusCode(200)
                .body("data.id", notNullValue());
    }

    // ── LIST ──

    @Test
    @Order(25)
    void listLoans_asAdmin_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get(PATH_LOANS)
                .then()
                .statusCode(200)
                .body("data.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(26)
    void listLoans_activeFilter_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .queryParam("active", true)
                .when()
                .get(PATH_LOANS)
                .then()
                .statusCode(200)
                .body("data.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(27)
    void listLoans_byMemberId_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .queryParam("memberId", memberId)
                .when()
                .get(PATH_LOANS)
                .then()
                .statusCode(200)
                .body("data.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(28)
    void listLoans_asUser_returns403() {
        given()
                .auth().preemptive().basic(USER_USER, USER_PASS)
                .when()
                .get(PATH_LOANS)
                .then()
                .statusCode(403);
    }

    // ── RETURN ──

    @Test
    @Order(30)
    void returnBook_returns200() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .post(PATH_LOANS + "/" + loanId + "/return")
                .then()
                .statusCode(200)
                .body("data.returnedAt", notNullValue())
                .body("data.id", is(loanId.intValue()));
    }

    @Test
    @Order(31)
    void returnBook_alreadyReturned_returns409() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .post(PATH_LOANS + "/" + loanId + "/return")
                .then()
                .statusCode(409)
                .body("errorCode", is("BORROWING_RULE_VIOLATION"));
    }

    @Test
    @Order(32)
    void returnBook_notFound_returns404() {
        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .post(PATH_LOANS + "/99999/return")
                .then()
                .statusCode(404)
                .body("errorCode", is("ENTITY_NOT_FOUND"));
    }

    // ── DELETE CONSTRAINTS ──

    @Test
    @Order(40)
    void deleteBook_withActiveLoan_returns409() {
        Long lendingBook = TestFixtures.createBook("Lent Book", "Author", "978-LN-LENT", 2);
        Long lendingMember = TestFixtures.createMember("Borrower Del", "loan-test-del@example.com");
        TestFixtures.borrowBook(lendingBook, lendingMember);

        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .delete(PATH_BOOKS + "/" + lendingBook)
                .then()
                .statusCode(409)
                .body("errorCode", is("ACTIVE_LOAN_CONFLICT"));
    }

    @Test
    @Order(41)
    void deleteMember_withActiveLoan_returns409() {
        Long delBook = TestFixtures.createBook("DelMem Book", "Author", "978-LN-DELM", 2);
        Long delMember = TestFixtures.createMember("Borrow DelM", "loan-test-delm@example.com");
        TestFixtures.borrowBook(delBook, delMember);

        given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .delete(PATH_MEMBERS + "/" + delMember)
                .then()
                .statusCode(409)
                .body("errorCode", is("ACTIVE_LOAN_CONFLICT"));
    }
}
