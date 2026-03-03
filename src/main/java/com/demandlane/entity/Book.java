package com.demandlane.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book")
public class Book extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String title;

    public String author;

    @Column(unique = true)
    public String isbn;

    @Column(name = "total_copies")
    public int totalCopies;

    @Column(name = "available_copies")
    public int availableCopies;

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public boolean hasActiveLoans() {
        return availableCopies < totalCopies;
    }

    public void checkout() {
        if (!isAvailable()) {
            throw new IllegalStateException("No available copies of book: " + title);
        }
        availableCopies--;
    }

    public void checkin() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies already returned for book: " + title);
        }
        availableCopies++;
    }

    public void adjustCopies(int newTotalCopies) {
        int diff = newTotalCopies - this.totalCopies;
        this.totalCopies = newTotalCopies;
        this.availableCopies = Math.max(0, this.availableCopies + diff);
    }
}
