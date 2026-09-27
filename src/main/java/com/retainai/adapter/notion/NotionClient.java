package com.retainai.adapter.notion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper around Notion's REST API (https://developers.notion.com).
 * Keep this class dumb: raw HTTP in, raw JSON-ish responses out. All
 * interpretation of your specific schema belongs in NotionContentSource.
 */
@Component
public class NotionClient {

    private final RestClient restClient;
    private final String databaseId;

    public NotionClient(
            @Value("${notion.api-token}") String apiToken,
            @Value("${notion.database-id}") String databaseId
    ) {
        this.databaseId = databaseId;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();
    }

    /**
     * POST /v1/databases/{database_id}/query
     * TODO: add a filter for your "ready to review" property, handle
     * pagination via the response's has_more/next_cursor fields.
     */
    public String queryDatabase() {
        return restClient.post()
                .uri("/databases/{id}/query", databaseId)
                .retrieve()
                .body(String.class);
    }

    /**
     * PATCH /v1/pages/{page_id}
     * TODO: build the property-update payload for your Status field.
     */
    public void updatePageStatus(String pageId, String status) {
        // TODO: implement PATCH request with updated Status property
    }
}
