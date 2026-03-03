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
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LoanService {

    private static final Logger LOG = Logger.getLogger(LoanService.class);

    private static final List<Function<BorrowContext, Optional<String>>> BORROWING_RULES = List.of(
            ctx -> !ctx.book().isAvailable()
                    ? Optional.of("No available copies of book: " + ctx.book().title)
                    : Optional.empty(),
            ctx -> ctx.activeLoans() >= ctx.config().maxActiveLoans()
                    ? Optional.of("Member has reached the maximum of " + ctx.config().maxActiveLoans() + " active loans")
                    : Optional.empty(),
            ctx -> ctx.hasOverdueLoans()
                    ? Optional.of("Member has overdue loans and cannot borrow new books")
                    : Optional.empty()
    );

    @Inject
    LoanRepository loanRepository;

    @Inject
    BookRepository bookRepository;

    @Inject
    MemberRepository memberRepository;

    @Inject
    LendingConfig lendingConfig;

    @Inject
    MeterRegistry meterRegistry;

    public List<Loan> listAll() {
        return loanRepository.listAll();
    }

    public List<Loan> listByMemberId(Long memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    public List<Loan> listActive() {
        return loanRepository.findActive();
    }

    public Loan findById(Long id) {
        Loan loan = loanRepository.findById(id);
        if (loan == null) {
            throw new EntityNotFoundException("Loan", id);
        }
        return loan;
    }

    @Transactional
    public Loan borrowBook(Long bookId, Long memberId) {
        Book book = bookRepository.findById(bookId);
        if (book == null) {
            throw new EntityNotFoundException("Book", bookId);
        }

        Member member = memberRepository.findById(memberId);
        if (member == null) {
            throw new EntityNotFoundException("Member", memberId);
        }

        BorrowContext ctx = new BorrowContext(
                book, member,
                loanRepository.countActiveByMemberId(memberId),
                loanRepository.hasOverdueLoans(memberId, LocalDate.now()),
                lendingConfig
        );

        BORROWING_RULES.stream()
                .map(rule -> rule.apply(ctx))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(msg -> { throw new BorrowingRuleException(msg); });

        book.checkout();

        Loan loan = new Loan();
        loan.book = book;
        loan.member = member;
        loan.borrowedAt = LocalDateTime.now();
        loan.dueDate = LocalDate.now().plusDays(lendingConfig.maxLoanDurationDays());
        loanRepository.persist(loan);

        meterRegistry.counter("lending.borrows").increment();
        LOG.infof("Book '%s' borrowed by member '%s', due %s", book.title, member.name, loan.dueDate);
        return loan;
    }

    @Transactional
    public Loan returnBook(Long loanId) {
        Loan loan = findById(loanId);
        if (loan.returnedAt != null) {
            throw new BorrowingRuleException("Loan has already been returned");
        }

        loan.returnedAt = LocalDateTime.now();
        loan.book.checkin();

        meterRegistry.counter("lending.returns").increment();
        LOG.infof("Book '%s' returned by member '%s'", loan.book.title, loan.member.name);
        return loan;
    }
}
