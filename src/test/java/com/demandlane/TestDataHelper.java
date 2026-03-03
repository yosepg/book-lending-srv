package com.demandlane;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TestDataHelper {

    @Inject
    EntityManager em;

    @Transactional
    public void makeLoansOverdue(Long memberId) {
        em.createNativeQuery("UPDATE loan SET due_date = '2020-01-01' WHERE member_id = :mid AND returned_at IS NULL")
                .setParameter("mid", memberId)
                .executeUpdate();
    }
}
