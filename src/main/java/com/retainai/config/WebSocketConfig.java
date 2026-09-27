package com.retainai.config;

import com.retainai.service.ReviewOrchestrator;
import com.retainai.voice.GeminiLiveClient;
import com.retainai.voice.LiveVoiceSessionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ReviewOrchestrator reviewOrchestrator;
    private final GeminiLiveClient geminiLiveClient;

    public WebSocketConfig(ReviewOrchestrator reviewOrchestrator, GeminiLiveClient geminiLiveClient) {
        this.reviewOrchestrator = reviewOrchestrator;
        this.geminiLiveClient = geminiLiveClient;
    }

    @Bean
    public LiveVoiceSessionHandler liveVoiceSessionHandler() {
        return new LiveVoiceSessionHandler(reviewOrchestrator, geminiLiveClient);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveVoiceSessionHandler(), "/ws/review")
                .setAllowedOrigins("*"); // TODO: restrict before shipping publicly
    }
}
