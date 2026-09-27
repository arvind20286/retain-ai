package com.retainai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single question/answer pair generated from an Article, plus everything
 * the scheduler needs. This replaces the review-state columns from the old
 * Notion schema (Status, Level, Last Reviewed, Next Review, No. of times
 * practiced, Is Due) — all of it lives here now, updated on every review,
 * independent of which note app the source Article came from.
 */
@Entity
@Table(name = "review_cards")
@Getter
@Setter
@NoArgsConstructor
public class ReviewCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(columnDefinition = "TEXT")
    private String question;

    /** The source snippet the grader checks your spoken answer against. */
    @Column(columnDefinition = "TEXT")
    private String referenceAnswer;

    // --- FSRS scheduling state (fed by an existing FSRS library, not hand-rolled) ---
    private double stability;
    private double difficulty;
    private Instant lastReviewedAt;
    private Instant nextReviewAt;
    private int timesReviewed;

    public boolean isDue() {
        return nextReviewAt == null || !nextReviewAt.isAfter(Instant.now());
    }
}
