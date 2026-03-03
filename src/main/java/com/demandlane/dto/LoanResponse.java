package com.demandlane.dto;

import com.demandlane.entity.Loan;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        LocalDateTime borrowedAt,
        LocalDate dueDate,
        LocalDateTime returnedAt
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.id,
                loan.book.id, loan.book.title,
                loan.member.id, loan.member.name,
                loan.borrowedAt, loan.dueDate, loan.returnedAt
        );
    }
}
