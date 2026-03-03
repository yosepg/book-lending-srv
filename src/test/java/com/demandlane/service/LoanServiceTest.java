package com.demandlane.service;

import com.demandlane.config.LendingConfig;
import com.demandlane.entity.Book;
import com.demandlane.entity.Loan;
import com.demandlane.entity.Member;
import com.demandlane.exception.BorrowingRuleException;
import com.demandlane.exception.EntityNotFoundException;
import com.demandlane.repository.BookRepository;
import com.demandlane.repository.LoanRepository;
import com.demandlane.repository.MemberRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class LoanServiceTest {

    @Inject
    LoanService loanService;

    @Inject
    LendingConfig lendingConfig;

    @InjectMock
    BookRepository bookRepository;

    @InjectMock
    MemberRepository memberRepository;

    @InjectMock
    LoanRepository loanRepository;

    private Book testBook;
    private Member testMember;

    @BeforeEach
    void setup() {
        testBook = new Book();
        testBook.id = 1L;
        testBook.title = "Test Book";
        testBook.totalCopies = 3;
        testBook.availableCopies = 2;

        testMember = new Member();
        testMember.id = 1L;
        testMember.name = "Test Member";
        testMember.email = "test@example.com";
    }

    private void stubHappyPath() {
        when(bookRepository.findById(1L)).thenReturn(testBook);
        when(memberRepository.findById(1L)).thenReturn(testMember);
        when(loanRepository.countActiveByMemberId(1L)).thenReturn(0L);
        when(loanRepository.hasOverdueLoans(eq(1L), any(LocalDate.class))).thenReturn(false);
    }

    @Test
    void borrowBook_happyPath_createsLoan() {
        stubHappyPath();

        Loan loan = loanService.borrowBook(1L, 1L);

        assertNotNull(loan);
        assertEquals(testBook, loan.book);
        assertEquals(testMember, loan.member);
        assertNotNull(loan.borrowedAt);
        assertEquals(LocalDate.now().plusDays(lendingConfig.maxLoanDurationDays()), loan.dueDate);
        assertEquals(1, testBook.availableCopies);
        verify(loanRepository).persist(any(Loan.class));
    }

    @Test
    void borrowBook_bookNotFound_throws() {
        when(bookRepository.findById(999L)).thenReturn(null);

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> loanService.borrowBook(999L, 1L));
        assertEquals("Book not found with id: 999", ex.getMessage());
    }

    @Test
    void borrowBook_memberNotFound_throws() {
        when(bookRepository.findById(1L)).thenReturn(testBook);
        when(memberRepository.findById(999L)).thenReturn(null);

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> loanService.borrowBook(1L, 999L));
        assertEquals("Member not found with id: 999", ex.getMessage());
    }

    @Test
    void borrowBook_noCopiesAvailable_throws() {
        testBook.availableCopies = 0;
        when(bookRepository.findById(1L)).thenReturn(testBook);
        when(memberRepository.findById(1L)).thenReturn(testMember);
        when(loanRepository.countActiveByMemberId(1L)).thenReturn(0L);
        when(loanRepository.hasOverdueLoans(eq(1L), any(LocalDate.class))).thenReturn(false);

        BorrowingRuleException ex = assertThrows(BorrowingRuleException.class,
                () -> loanService.borrowBook(1L, 1L));
        assertEquals("No available copies of book: Test Book", ex.getMessage());
    }

    @Test
    void borrowBook_maxLoansReached_throws() {
        when(bookRepository.findById(1L)).thenReturn(testBook);
        when(memberRepository.findById(1L)).thenReturn(testMember);
        when(loanRepository.countActiveByMemberId(1L)).thenReturn((long) lendingConfig.maxActiveLoans());
        when(loanRepository.hasOverdueLoans(eq(1L), any(LocalDate.class))).thenReturn(false);

        BorrowingRuleException ex = assertThrows(BorrowingRuleException.class,
                () -> loanService.borrowBook(1L, 1L));
        assertEquals("Member has reached the maximum of " + lendingConfig.maxActiveLoans() + " active loans",
                ex.getMessage());
    }

    @Test
    void borrowBook_overdueLoans_throws() {
        when(bookRepository.findById(1L)).thenReturn(testBook);
        when(memberRepository.findById(1L)).thenReturn(testMember);
        when(loanRepository.countActiveByMemberId(1L)).thenReturn(0L);
        when(loanRepository.hasOverdueLoans(eq(1L), any(LocalDate.class))).thenReturn(true);

        BorrowingRuleException ex = assertThrows(BorrowingRuleException.class,
                () -> loanService.borrowBook(1L, 1L));
        assertEquals("Member has overdue loans and cannot borrow new books", ex.getMessage());
    }

    @Test
    void returnBook_happyPath_setsReturnedAt() {
        Loan loan = new Loan();
        loan.id = 1L;
        loan.book = testBook;
        loan.member = testMember;
        loan.borrowedAt = LocalDateTime.now().minusDays(5);
        loan.dueDate = LocalDate.now().plusDays(9);
        loan.returnedAt = null;
        testBook.availableCopies = 1;

        when(loanRepository.findById(1L)).thenReturn(loan);

        Loan returned = loanService.returnBook(1L);

        assertNotNull(returned.returnedAt);
        assertEquals(2, testBook.availableCopies);
    }

    @Test
    void returnBook_alreadyReturned_throws() {
        Loan loan = new Loan();
        loan.id = 1L;
        loan.book = testBook;
        loan.member = testMember;
        loan.returnedAt = LocalDateTime.now();

        when(loanRepository.findById(1L)).thenReturn(loan);

        BorrowingRuleException ex = assertThrows(BorrowingRuleException.class,
                () -> loanService.returnBook(1L));
        assertEquals("Loan has already been returned", ex.getMessage());
    }

    @Test
    void returnBook_notFound_throws() {
        when(loanRepository.findById(999L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> loanService.returnBook(999L));
    }
}
