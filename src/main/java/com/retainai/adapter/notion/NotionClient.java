package com.retainai.adapter.notion;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Stream;

/**
 * Thin wrapper around Notion's REST API (https://developers.notion.com).
 * Keep this class dumb: raw HTTP in, raw JSON-ish responses out. All
 * interpretation of your specific schema belongs in NotionContentSource.
 */
@Component
public class NotionClient {

    private static final Logger log = LoggerFactory.getLogger(NotionClient.class);

    private final RestClient restClient;
    private final String datasourceId;

    public NotionClient(
            @Value("${notion.api-token}") String apiToken,
            @Value("${notion.datasource-id}") String datasourceId
    ) {
        this.datasourceId = datasourceId;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .defaultHeader("Notion-Version", "2026-03-11")
                .requestInterceptor((request, body, execution) -> {
                    log.info("Notion request URI: {}", request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * POST /v1/databases/{database_id}/query
     * TODO: add a filter for your "ready to review" property, handle
     * pagination via the response's has_more/next_cursor fields.
     */
    public String queryDatasource(List<String> filterProperties) {
        String uri = "/data_sources/{id}/query";
        if(filterProperties != null && !filterProperties.isEmpty()) {
            Stream<String> filterStream = filterProperties.stream().map(prop -> "filter_properties[]=" + prop);
            uri += "?" + filterStream.reduce((a, b) -> a + "&" + b).orElse("");
        }
        return restClient.post()
                .uri(uri, datasourceId)
                .retrieve()
                .body(String.class);
    }

    public String getPageAsMarkdown(String pageId) {
        String uri = "/pages/{page_id}/markdown";
        return restClient.get()
                .uri(uri, pageId)
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
