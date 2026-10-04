package com.retainai.config;

import com.retainai.service.ReviewOrchestrator;
import com.retainai.voice.GeminiLiveClient;
import com.retainai.voice.LiveVoiceSessionHandler;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private ReviewOrchestrator reviewOrchestrator;
    private String apiKey;
    private String modelName;
    private String systemPromptPath;

    public WebSocketConfig(@Value("${gemini.model-name}") String modelId, @Value("${gemini.api-key}") String apiKey, @Value("${gemini.system-prompt:}") String systemPromptPath, ReviewOrchestrator reviewOrchestrator){
        this.reviewOrchestrator = reviewOrchestrator;
        this.apiKey = apiKey;
        this.modelName = modelId;
        this.systemPromptPath = systemPromptPath;
    }

    @Bean
    public LiveVoiceSessionHandler liveVoiceSessionHandler() {
        return new LiveVoiceSessionHandler(this.modelName, this.apiKey, this.systemPromptPath, this.reviewOrchestrator);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveVoiceSessionHandler(), "/ws/review")
                .setAllowedOrigins("*"); // TODO: restrict before shipping publicly
    }
}
