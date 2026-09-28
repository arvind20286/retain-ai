package com.retainai.adapter.notion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.retainai.adapter.ContentSource;
import com.retainai.domain.Article;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Reads articles from a Notion database and maps them to the internal
 * {@link Article} model. This is deliberately thin: all it does is fetch
 * + map. It does NOT own review state (status/level/next-review) — that
 * lives in our own DB (see {@code repository} package) once synced.
 *
 * TODO: inject NotionClient (thin wrapper around Notion's REST API + your
 * integration token), map your existing schema:
 *   - "Resource Link"   -> Article.sourceUrl
 *   - "Reading List"    -> Article.title
 *   - "Tag"             -> Article.tags
 *   - "Insight"         -> Article.userNotes (useful context for question gen)
 * Fields like Status/Level/Last Reviewed/Next Review/Is Due are NOT read here —
 * those become our own DB's responsibility going forward (see design note in
 * repository.ReviewCardRepository).
 */
@Component
public class NotionContentSource implements ContentSource {

    private final NotionClient notionClient;

    public NotionContentSource(NotionClient notionClient) {
        this.notionClient = notionClient;
    }

    @Override
    public String sourceName() {
        return "notion";
    }

    @Override
    public List<Article> fetchReadyArticles() throws JsonProcessingException {
        // TODO: query the Notion database, filter to your "ready to review"
        // checkbox/status, map each page to an Article via NotionClient.
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> filterProperties = List.of("Topic", "Resource Link", "Tag");
        JsonNode notionResponse = objectMapper.readTree(notionClient.queryDatabase(null));
        for (JsonNode page : notionResponse.get("results")) {
            String pageId = page.get("id").asText();
//            String title = page.get("properties").get("Reading List").get("title").get(0).get("text").get("content").asText();
//            String sourceUrl = page.get("properties").get("Resource Link").get("url").asText();
//            List<String> tags = objectMapper.convertValue(page.get("properties").get("Tag").get("multi_select"), List.class);

//            Article article = new Article();
//            article.setSourceName(sourceName());
//            article.setExternalId(pageId);
//            article.setTitle(title);
//            article.setSourceUrl(sourceUrl);
//            article.setTags(tags);

            // Fetch the content as Markdown
            String markdownResponse = notionClient.getPageAsMarkdown(pageId);
            ObjectMapper markdownMapper = new ObjectMapper();
            JsonNode markdownNode = markdownMapper.readTree(markdownResponse);
            String markdownContent = markdownNode.get("markdown").asText();
            System.out.println("Fetched Markdown content for page " + pageId + ": " + markdownContent.substring(0, Math.min(100, markdownContent.length())) + "...");
//            article.setContent(markdownContent);
        }
        throw new UnsupportedOperationException("TODO: implement Notion fetch + mapping");
    }

    @Override
    public void writeBackStatus(String externalId, String status) {
        // TODO: PATCH the Notion page's Status property via NotionClient.
        // This is the one write we keep in Notion, for visibility only —
        // detailed review history stays in our own DB.
    }
}
