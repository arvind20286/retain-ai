package com.retainai.service;

import com.retainai.domain.GradeResult;
import com.retainai.domain.ReviewCard;
import com.retainai.repository.ReviewCardRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Wraps an existing FSRS (Free Spaced Repetition Scheduler) implementation —
 * do not hand-derive the interval math yourself, it's a solved problem and
 * a from-scratch version will likely perform worse.
 *
 * TODO: pull in an FSRS Java port (or port the reference algorithm — it's
 * a compact, well-documented formula) and call it from applyGrade().
 */
@Service
public class SchedulerService {

    private final ReviewCardRepository reviewCardRepository;

    public SchedulerService(ReviewCardRepository reviewCardRepository) {
        this.reviewCardRepository = reviewCardRepository;
    }

    public List<ReviewCard> findDueCards() {
        return reviewCardRepository.findDue(Instant.now());
    }

    /**
     * Feeds the grading score into FSRS to update stability/difficulty and
     * compute the next review date, then persists the card.
     */
    public void applyGrade(ReviewCard card, GradeResult grade) {
        // TODO: map grade.score() to an FSRS "rating" (again/hard/good/easy
        // equivalent), run the FSRS update step, set card.stability/difficulty/
        // nextReviewAt accordingly.
        card.setTimesReviewed(card.getTimesReviewed() + 1);
        card.setLastReviewedAt(Instant.now());
        reviewCardRepository.save(card);
    }
}
