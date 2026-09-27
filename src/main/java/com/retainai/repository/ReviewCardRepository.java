package com.retainai.repository;

import com.retainai.domain.ReviewCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

/**
 * This repository is now the source of truth for everything that used to be
 * Notion properties (Status, Level, Last Reviewed, Next Review, Is Due,
 * No. of times practiced). Notion only ever gets a summary write-back
 * (see ContentSource#writeBackStatus) — it is never queried for "what's due."
 */
public interface ReviewCardRepository extends JpaRepository<ReviewCard, Long> {

    @Query("select c from ReviewCard c where c.nextReviewAt is null or c.nextReviewAt <= :now")
    List<ReviewCard> findDue(Instant now);
}
