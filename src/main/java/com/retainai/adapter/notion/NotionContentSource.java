package com.retainai.adapter.notion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.retainai.adapter.ContentSource;
import com.retainai.domain.Article;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
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
        List<Article> articleList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> filterProperties = List.of("Topic", "Resource Link", "Tag");
        JsonNode notionResponse = objectMapper.readTree(notionClient.queryDatasource(null));
        for (JsonNode page : notionResponse.get("results")) {
            String pageId = page.get("id").asText();
            JsonNode properties = page.get("properties");
            String topic = properties.get("Topic").get("title").get(0).get("plain_text").asText("");
            if(topic.isEmpty()){
                System.out.println("Topic not present for page-id: "+pageId);
            }
            String sourceUrl = properties.get("Resource Link").get("url").asText("");

            JsonNode multiSelect = properties.get("Tag").get("multi_select");

            List<String> tags = new ArrayList<>();
            if(multiSelect.isArray()){
                for(JsonNode select : multiSelect){
                    tags.add(select.get("name").asText(""));
                }
            }

            String markdownResponse = notionClient.getPageAsMarkdown(pageId);
            ObjectMapper markdownMapper = new ObjectMapper();
            JsonNode markdownNode = markdownMapper.readTree(markdownResponse);
            String markdownContent = markdownNode.get("markdown").asText("");
            System.out.println("Fetched Markdown content for page " + pageId + ": " + markdownContent.substring(0, Math.min(100, markdownContent.length())) + "...");
            Article article = new Article();
            article.setPageId(pageId);
            article.setTitle(topic);
            if(!tags.isEmpty()){
                article.setTags(tags);
            }
            if(!sourceUrl.isEmpty()) {
                article.setSourceUrl(sourceUrl);
            }
            if(!markdownContent.isEmpty()) {
                article.setContent(markdownContent);
            }
            article.setSourceName(this.sourceName());
            articleList.add(article);
        }
        return articleList;
    }

    @Override
    public void writeBackStatus(String externalId, String status) {
        // TODO: PATCH the Notion page's Status property via NotionClient.
        // This is the one write we keep in Notion, for visibility only —
        // detailed review history stays in our own DB.
    }
}
