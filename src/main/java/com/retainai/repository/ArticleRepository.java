package com.retainai.repository;

import com.retainai.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySourceNameAndExternalId(String sourceName, String externalId);
}
