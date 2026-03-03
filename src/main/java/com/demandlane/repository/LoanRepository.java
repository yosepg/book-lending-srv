package com.demandlane.repository;

import com.demandlane.entity.Loan;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class LoanRepository implements PanacheRepository<Loan> {

    public long countActiveByMemberId(Long memberId) {
        return count("member.id = ?1 and returnedAt is null", memberId);
    }

    public boolean hasOverdueLoans(Long memberId, LocalDate asOf) {
        return count("member.id = ?1 and returnedAt is null and dueDate < ?2", memberId, asOf) > 0;
    }

    public List<Loan> findByMemberId(Long memberId) {
        return list("member.id", memberId);
    }

    public List<Loan> findActive() {
        return list("returnedAt is null");
    }
}
