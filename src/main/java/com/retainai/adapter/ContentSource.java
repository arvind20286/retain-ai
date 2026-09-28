package com.retainai.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.retainai.domain.Article;

import java.util.List;

/**
 * A source of raw content to turn into review material.
 *
 * This is the seam that keeps the review engine (question generation, grading,
 * scheduling) completely decoupled from *where* the articles come from.
 * Notion is the only implementation built in v1 (see {@code adapter.notion}),
 * but adding Obsidian, Readwise, or plain markdown files later means writing
 * one more class here — not touching anything downstream.
 */
public interface ContentSource {

    /**
     * A short identifier for this source, e.g. "notion", "obsidian".
     * Used for logging and for tagging which source an Article came from.
     */
    String sourceName();

    /**
     * Pulls all articles currently marked ready-to-review in this source.
     * Implementations own their own pagination/rate-limit handling; callers
     * just get a flat list back.
     */
    List<Article> fetchReadyArticles() throws JsonProcessingException;

    /**
     * Optional: push review status back to the source (e.g. update a Notion
     * page property). Not every source needs to support this — default is a
     * no-op so read-only sources don't have to implement it.
     */
    default void writeBackStatus(String externalId, String status) {
        // no-op by default
    }
}
