package com.retainai.service;

import com.retainai.domain.Article;
import com.retainai.domain.ReviewCard;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calls an LLM to extract 3-5 question/reference-answer pairs per Article.
 * TODO: prompt the model to return structured JSON (question + referenceAnswer
 * pairs) so this doesn't need fragile text parsing. Log raw model output
 * somewhere while you're tuning the prompt — that log is useful evidence
 * later for explaining how you validated quality.
 */
@Service
public class QuestionGeneratorService {

    public List<ReviewCard> generateCardsFor(Article article) {
        // TODO: call Gemini/genai client with article.getContent() (+ userNotes
        // for extra context), parse structured response, build ReviewCard list
        // with article set, stability/difficulty at FSRS defaults, nextReviewAt
        // = now (so new cards are immediately due).
        throw new UnsupportedOperationException("TODO: implement LLM question generation");
    }
}
