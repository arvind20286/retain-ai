package com.retainai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.retainai.adapter.notion.NotionContentSource;
import com.retainai.domain.Article;
import com.retainai.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ArticleRepository articleRepository;
    private final NotionContentSource notionContentSource;

    public void syncArticles(){

        try {
            List<Article> articleList = notionContentSource.fetchReadyArticles();
            articleList.forEach(articleRepository::save);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
