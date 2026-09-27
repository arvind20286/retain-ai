package com.retainai.service;

import com.retainai.domain.GradeResult;
import com.retainai.domain.ReviewCard;
import org.springframework.stereotype.Service;

/**
 * This is the piece worth going deep on — see the build plan's note about
 * making this your resume talking point.
 *
 * TODO:
 *  1. Prompt the LLM with: the question, the reference answer/source snippet,
 *     and the transcribed spoken answer. Ask for structured JSON matching
 *     GradeResult (score 0-100, missedKeyPoint bool, one-line feedback).
 *  2. Build a small fixed test set of answers you know are right, wrong, and
 *     partially right. Run them through here and check the scores agree with
 *     your own judgment. Iterate the prompt until they do — keep a log of
 *     prompt versions and how scores changed, it's good interview material.
 *  3. Be explicit in the prompt about partial credit — a rambling but
 *     substantively correct answer should score higher than a short,
 *     confident, wrong one. This is the part self-graded flashcard apps
 *     get wrong, and it's your actual differentiator.
 */
@Service
public class GradingService {

    public GradeResult grade(ReviewCard card, String transcribedAnswer) {
        throw new UnsupportedOperationException("TODO: implement LLM-based grading call");
    }
}
