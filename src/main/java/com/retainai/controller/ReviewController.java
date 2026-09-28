package com.retainai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.retainai.adapter.ContentSource;
import com.retainai.domain.ReviewCard;
import com.retainai.service.ReviewOrchestrator;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ContentSource notionContentSource; // Spring injects NotionContentSource here
    private final ReviewOrchestrator reviewOrchestrator;

    public ReviewController(ContentSource notionContentSource, ReviewOrchestrator reviewOrchestrator) {
        this.notionContentSource = notionContentSource;
        this.reviewOrchestrator = reviewOrchestrator;
    }

    /** Manually trigger a Notion -> local DB sync (cron this later). */
    @PostMapping("/sync")
    public void triggerSync() {
        // TODO: fetch via notionContentSource.fetchReadyArticles(), upsert into
        // ArticleRepository, generate ReviewCards for any new articles via
        // QuestionGeneratorService.
        try {
            notionContentSource.fetchReadyArticles();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /** What's due right now — drives the voice session's question queue. */
    @GetMapping("/cards/due")
    public List<ReviewCard> dueCards() {
        return reviewOrchestrator.nextCardsForSession();
    }
}
