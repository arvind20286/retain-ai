package com.retainai.domain;

/**
 * Structured result from grading a spoken (transcribed) answer against a
 * ReviewCard's reference answer. Keep this a plain record — it's the
 * contract between GradingService and SchedulerService, and the piece
 * you'll iterate on most while tuning your grading prompt.
 */
public record GradeResult(
        int score,          // 0-100
        boolean missedKeyPoint,
        String feedback      // one-line human-readable verdict
) {}
