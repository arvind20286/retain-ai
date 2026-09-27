package com.retainai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Content only — deliberately has no review-state fields (status, level,
 * next review, etc). Those live on {@link ReviewCard}. This split is what
 * lets us swap or add note-app sources without touching scheduling logic.
 */
@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which ContentSource this came from, e.g. "notion". */
    private String sourceName;

    /** The source system's own id for this item (Notion page id, etc). */
    private String externalId;

    private String title;
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String userNotes; // maps from your "Insight" property

    @ElementCollection
    private List<String> tags;
}
