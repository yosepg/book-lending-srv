package com.demandlane.service;

import com.demandlane.config.LendingConfig;
import com.demandlane.entity.Book;
import com.demandlane.entity.Member;

record BorrowContext(
        Book book,
        Member member,
        long activeLoans,
        boolean hasOverdueLoans,
        LendingConfig config
) {}
