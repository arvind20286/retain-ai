package com.retainai.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wraps com.google.genai's Live API session handling (audio, video, text,
 * and session resumption are supported in the Java SDK). Keep this class
 * focused on the Gemini session lifecycle only; LiveVoiceSessionHandler owns
 * bridging that lifecycle to the browser's WebSocket.
 *
 * TODO: see the SDK's Live API docs/samples for the current Client + session
 * builder API shape (this evolves between SDK versions — check the version
 * pinned in pom.xml against googleapis/java-genai's README examples).
 */
@Component
public class GeminiLiveClient {

    private final String apiKey;

    public GeminiLiveClient(@Value("${gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    // TODO: openSession(String openingQuestionText) -> live session handle
    // TODO: sendAudioChunk(byte[] pcmAudio)
    // TODO: onTranscript(Consumer<String> callback)
    // TODO: closeSession()
}
