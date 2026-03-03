package com.demandlane.service;

import com.demandlane.dto.MemberRequest;
import com.demandlane.entity.Member;
import com.demandlane.exception.ActiveLoanException;
import com.demandlane.exception.DuplicateEntityException;
import com.demandlane.exception.EntityNotFoundException;
import com.demandlane.repository.LoanRepository;
import com.demandlane.repository.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class MemberService {

    @Inject
    MemberRepository memberRepository;

    @Inject
    LoanRepository loanRepository;

    public List<Member> listAll() {
        return memberRepository.listAll();
    }

    public Member findById(Long id) {
        Member member = memberRepository.findById(id);
        if (member == null) {
            throw new EntityNotFoundException("Member", id);
        }
        return member;
    }

    @Transactional
    public Member create(MemberRequest request) {
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEntityException("A member with email " + request.email() + " already exists");
        }
        Member member = new Member();
        member.name = request.name();
        member.email = request.email();
        memberRepository.persist(member);
        return member;
    }

    @Transactional
    public Member update(Long id, MemberRequest request) {
        Member member = findById(id);
        member.name = request.name();
        member.email = request.email();
        return member;
    }

    @Transactional
    public void delete(Long id) {
        Member member = findById(id);
        long activeLoans = loanRepository.countActiveByMemberId(id);
        if (activeLoans > 0) {
            throw new ActiveLoanException("Cannot delete member with active loans");
        }
        memberRepository.delete(member);
    }
}
