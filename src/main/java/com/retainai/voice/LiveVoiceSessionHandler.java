package com.retainai.voice;

import com.retainai.service.ReviewOrchestrator;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;

/**
 * Bridges the browser's WebSocket connection (mic audio in, spoken response
 * audio out) to the Gemini Live API session (see GeminiLiveClient).
 *
 * Build/debug order matters: get this working end-to-end with ONE hardcoded
 * question before wiring in real cards from ReviewOrchestrator — isolate the
 * hardest technical piece (audio streaming + session lifecycle) early.
 *
 * TODO:
 *  - On connection open: pick the next due card via ReviewOrchestrator,
 *    open a GeminiLiveClient session, send the question as the model's
 *    opening turn (text-to-speech happens on Gemini's side).
 *  - Stream incoming binary audio frames from the browser to the Live session.
 *  - On turn complete, take the transcript from the Live session and call
 *    ReviewOrchestrator.submitAnswer(...).
 *  - Handle session resumption if the WebSocket drops mid-answer — the
 *    genai Java SDK supports this, don't just let a dropped connection
 *    lose the review.
 */
public class LiveVoiceSessionHandler extends BinaryWebSocketHandler {

    private final ReviewOrchestrator reviewOrchestrator;
    private final GeminiLiveClient geminiLiveClient;

    public LiveVoiceSessionHandler(ReviewOrchestrator reviewOrchestrator, GeminiLiveClient geminiLiveClient) {
        this.reviewOrchestrator = reviewOrchestrator;
        this.geminiLiveClient = geminiLiveClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        // TODO: start the review session for this WebSocket connection
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        // TODO: clean up the Gemini Live session if still open
    }
}
