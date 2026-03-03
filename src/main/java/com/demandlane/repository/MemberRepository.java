package com.demandlane.repository;

import com.demandlane.entity.Member;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class MemberRepository implements PanacheRepository<Member> {

    public Optional<Member> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
