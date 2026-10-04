package com.retainai.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.AsyncSession;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.retainai.service.ReviewOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

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
public class LiveVoiceSessionHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveVoiceSessionHandler.class);

    private final ReviewOrchestrator reviewOrchestrator;
    private final Client geminiLiveClient;
    private String modelId;
    private String apiKey;
    private String systemPrompt;

    public LiveVoiceSessionHandler(String modelId, String apiKey, String systemPromptPath, ReviewOrchestrator reviewOrchestrator){
        this.reviewOrchestrator = reviewOrchestrator;
        this.modelId =  modelId;
        this.apiKey = apiKey;
        try {
            this.systemPrompt = new String(
                    Objects.requireNonNull(getClass().getResourceAsStream(systemPromptPath)).readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.geminiLiveClient = Client.builder().apiKey(apiKey).build();
    }

    public void onRecieve(WebSocketSession session, LiveServerMessage liveServerMessage){
        log.debug("Received message from Gemini Live: frontendSessionId={}", session.getId());
        ObjectMapper objectMapper =new ObjectMapper();

        try{
            if(liveServerMessage.setupComplete().isPresent()){
                log.info("Gemini Live setup complete: frontendSessionId={}", session.getId());
                String msg = objectMapper.writeValueAsString(
                        Map.of("type", "interaction_status", "status", "REQUIRES_ACTION")
                );
                if (session.isOpen()) session.sendMessage(new TextMessage(msg));
                return;
            }
            liveServerMessage.serverContent().ifPresent(serverContent -> {
                serverContent.modelTurn().ifPresent(modelTurn -> {
                    modelTurn.parts().ifPresent(parts -> parts.forEach(part -> {
                        part.text().ifPresent(text -> {
                            try {
                                log.info("Received text from Gemini Live: frontendSessionId={}, chars={}", session.getId(), text.length());
                                String gm = objectMapper.writeValueAsString(Map.of("type", "gemini", "text", text));
                                if (session.isOpen()) session.sendMessage(new TextMessage(gm));
                            } catch (Exception e) {
                                log.warn("Failed to forward Gemini text to frontend: sessionId={}", session.getId(), e);
                            }
                        });

                        part.inlineData().ifPresent(blob -> {
                            try {
                                if (blob.data().isPresent()) {
                                    byte[] pcm = blob.data().get();
                                    log.debug("Received audio from Gemini Live: frontendSessionId={}, bytes={}", session.getId(), pcm.length);
                                    if (session.isOpen()) {
                                        session.sendMessage(new BinaryMessage(pcm));
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Failed to forward Gemini audio to frontend: sessionId={}", session.getId(), e);
                            }
                        });
                    }));
                });

                if (serverContent.turnComplete().orElse(false)) {
                    log.info("Gemini Live turn complete: frontendSessionId={}", session.getId());
                    try {
                        String tc = objectMapper.writeValueAsString(Map.of("type", "turn_complete"));
                        if (session.isOpen()) session.sendMessage(new TextMessage(tc));
                    } catch (Exception e) {
                        // swallow
                    }
                }
            });

        }catch (Exception e) {
            log.warn("Failed to forward Gemini Live message to frontend: sessionId={}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        log.info("Frontend WebSocket connected: sessionId={}", session.getId());
        AtomicReference<AsyncSession> asyncRef = new AtomicReference<>();
        session.getAttributes().put("asyncRef", asyncRef);

        CompletableFuture.runAsync(
                () -> {
                    try {
                        LiveConnectConfig liveConnectConfig = LiveConnectConfig.builder().
                                systemInstruction(Content.fromParts(Part.fromText(systemPrompt))).build();
                        AsyncSession asyncSession = geminiLiveClient.async.live.connect(modelId, liveConnectConfig).get();
                        log.info("Gemini Live connected: frontendSessionId={}, model={}", session.getId(), modelId);

                        // Register a receiver that forwards server messages to the frontend websocket.
                        asyncSession
                                .receive(liveServerMessage -> {this.onRecieve(session, liveServerMessage);}).join();
                                        asyncRef.set(asyncSession);

                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Failed to connect to Gemini Live: frontendSessionId={}", session.getId(), e);
                        try {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage("{\"error\":\"Failed to connect to Gemini live: "
                                        + e.getMessage().replaceAll("\"", "\\\"") + "\"}"));
                                session.close(CloseStatus.SERVER_ERROR);
                            }
                        } catch (Exception ex) {
                            // swallow
                        }
                    }
                }
        ).exceptionally(t -> {
                        log.error("Gemini Live connection task failed: frontendSessionId={}", session.getId(), t);
                        try {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage("{\"error\":\"Live connection task failed: "
                                        + t.getMessage().replaceAll("\"", "\\\"") + "\"}"));
                                session.close(CloseStatus.SERVER_ERROR);
                            }
                        } catch (Exception e) {
                            // swallow
                        }
                        return null;
                    });

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("Received text message from frontend: sessionId={}, chars={}",
                session.getId(), payload == null ? 0 : payload.length());
        AtomicReference<AsyncSession> asyncRef = (AtomicReference<AsyncSession>) session.getAttributes().get("asyncRef");
        if (asyncRef == null || asyncRef.get() == null) {
            try {
                session.sendMessage(new TextMessage("{\"error\":\"Live session not ready yet\"}"));
            } catch (Exception e) {
                // ignore
            }
            return;
        }
        AsyncSession asyncSession = asyncRef.get();

        // Parse frontend JSON protocol: { text: "..." } or { type: "image", mime_type: "image/jpeg", data: "BASE64" }
        try {
            ObjectMapper mapper = new ObjectMapper();
            String textToSend = null;

            if (payload != null && payload.trim().startsWith("{")) {
                JsonNode node = mapper.readTree(payload);
                if (node.has("text")) {
                    textToSend = node.get("text").asText();
                } else if (node.has("type") && "image".equalsIgnoreCase(node.get("type").asText()) && node.has("data")) {
                    String mime = node.has("mime_type") ? node.get("mime_type").asText() : "image/jpeg";
                    String data = node.get("data").asText();
                    log.info("Received image from frontend: sessionId={}, mimeType={}, base64Chars={}",
                            session.getId(), mime, data.length());
                    // send data URI as a part; SDK examples accept Part.fromUri for images
                    String dataUri = "data:" + mime + ";base64," + data;

                    LiveSendClientContentParameters clientContent =
                            LiveSendClientContentParameters.builder()
                                    .turnComplete(true)
                                    .turns(Content.fromParts(Part.fromUri(dataUri, mime)))
                                    .build();

                    asyncSession
                            .sendClientContent(clientContent)
                            .exceptionally(t -> {
                                try {
                                    if (session.isOpen()) {
                                        session.sendMessage(new TextMessage("{\"error\":\"Failed to send client content: "
                                                + t.getMessage().replaceAll("\"", "\\\"") + "\"}"));
                                    }
                                } catch (Exception e) {
                                    // swallow
                                }
                                return null;
                            });
                    return;
                } else {
                    session.sendMessage(new TextMessage("{\"error\":\"Unsupported message format\"}"));
                    return;
                }
            } else {
                textToSend = payload;
            }

            if (textToSend != null) {
                LiveSendClientContentParameters clientContent =
                        LiveSendClientContentParameters.builder()
                                .turnComplete(true)
                                .turns(Content.fromParts(Part.fromText(textToSend)))
                                .build();

                asyncSession
                        .sendClientContent(clientContent)
                        .exceptionally(t -> {
                            try {
                                if (session.isOpen()) {
                                    session.sendMessage(new TextMessage("{\"error\":\"Failed to send client content: "
                                            + t.getMessage().replaceAll("\"", "\\\"") + "\"}"));
                                }
                            } catch (Exception e) {
                                // swallow
                            }
                            return null;
                        });
            }

        } catch (Exception e) {
            log.warn("Invalid JSON message from frontend: sessionId={}", session.getId(), e);
            try {
                session.sendMessage(new TextMessage("{\"error\":\"Invalid JSON payload\"}"));
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        log.debug("Received binary message from frontend: sessionId={}, bytes={}",
                session.getId(), message.getPayload().remaining());
        AtomicReference<AsyncSession> asyncRef =
                (AtomicReference<AsyncSession>) session.getAttributes().get("asyncRef");
        AsyncSession asyncSession = asyncRef == null ? null : asyncRef.get();
        if (asyncSession == null) {
            sendError(session, "Live session not ready yet");
            return;
        }
        java.nio.ByteBuffer payload = message.getPayload().slice();
        if (payload.remaining() == 0 || payload.remaining() % 2 != 0) {
            sendError(session, "Audio must be non-empty 16-bit PCM data");
            return;
        }

        byte[] audioBytes = new byte[payload.remaining()];
        payload.get(audioBytes);
        LiveSendRealtimeInputParameters audioInput =
                LiveSendRealtimeInputParameters.builder()
                        .audio(
                                Blob.builder()
                                        .data(audioBytes)
                                        .mimeType("audio/pcm;rate=16000")
                                        .build())
                        .build();

        asyncSession
                .sendRealtimeInput(audioInput)
                .exceptionally(
                        error -> {
                            sendError(session, "Failed to send audio to Gemini");
                            log.warn("Failed to send audio to Gemini Live: sessionId={}", session.getId(), error);
                            return null;
                        });
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                String json =
                        new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(java.util.Map.of("error", message));
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("Failed to send error to frontend: sessionId={}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        log.info("Frontend WebSocket disconnected: sessionId={}, closeCode={}, reason={}",
                session.getId(), status.getCode(), status.getReason());
        AtomicReference<AsyncSession> asyncRef = (AtomicReference<AsyncSession>) session.getAttributes().get("asyncRef");
        if (asyncRef != null && asyncRef.get() != null) {
            try {
                asyncRef.get().close().join();
                log.info("Gemini Live session closed: frontendSessionId={}", session.getId());
            } catch (Exception e) {
                log.warn("Failed to close Gemini Live session: frontendSessionId={}", session.getId(), e);
            }
        }
    }
}
