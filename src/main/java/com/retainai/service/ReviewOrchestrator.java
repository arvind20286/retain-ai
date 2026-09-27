package com.retainai.service;

import com.retainai.domain.GradeResult;
import com.retainai.domain.ReviewCard;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates a review session: picks due cards, and for each one, drives
 * the cycle of "ask question aloud -> receive transcribed answer -> grade ->
 * update schedule". Called by the voice WebSocket handler, which owns the
 * actual audio streaming; this class only deals with text/data.
 */
@Service
public class ReviewOrchestrator {

    private final SchedulerService schedulerService;
    private final GradingService gradingService;

    public ReviewOrchestrator(SchedulerService schedulerService, GradingService gradingService) {
        this.schedulerService = schedulerService;
        this.gradingService = gradingService;
    }

    public List<ReviewCard> nextCardsForSession() {
        return schedulerService.findDueCards();
    }

    /** Called once the voice layer has a transcript for the current card. */
    public GradeResult submitAnswer(ReviewCard card, String transcribedAnswer) {
        GradeResult result = gradingService.grade(card, transcribedAnswer);
        schedulerService.applyGrade(card, result);
        return result;
    }
}
