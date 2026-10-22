package com.procucev.transcription.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procucev.transcription.dto.LanguageDetectionResponse;
import com.procucev.transcription.dto.TranscriptionRequest;
import com.procucev.transcription.dto.TranscriptionResponse;
import com.procucev.transcription.service.AiTranscriptionService;
import com.procucev.transcription.service.TranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TranscriptionWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionWebSocketHandler.class);
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiTranscriptionService aiService;
    private final TranscriptionService transcriptionService;

    public TranscriptionWebSocketHandler(AiTranscriptionService aiService, TranscriptionService transcriptionService) {
        this.aiService = aiService;
        this.transcriptionService = transcriptionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        activeSessions.put(session.getId(), session);
        log.info("WebSocket connected: sessionId={}", session.getId());

        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "CONNECTED");
        welcome.put("sessionId", session.getId());
        welcome.put("message", "Connected to Real-time Multilingual Speech-to-Text WebSocket Stream");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String action = json.path("action").asText("STREAM_INTERIM");

            if ("STREAM_INTERIM".equalsIgnoreCase(action)) {
                // Client is streaming live speech text interim tokens
                String interimText = json.path("text").asText("");
                double duration = json.path("duration").asDouble(0.0);

                LanguageDetectionResponse langAnalysis = aiService.detectLanguage(interimText);

                Map<String, Object> reply = new HashMap<>();
                reply.put("type", "INTERIM_ANALYSIS");
                reply.put("text", interimText);
                reply.put("detectedLanguage", langAnalysis.getPrimaryLanguage());
                reply.put("isMultilingual", langAnalysis.isMultilingual());
                reply.put("hindiScore", langAnalysis.getHindiScore());
                reply.put("englishScore", langAnalysis.getEnglishScore());
                reply.put("hindiTokens", langAnalysis.getDetectedHindiTokens());
                reply.put("englishTokens", langAnalysis.getDetectedEnglishTokens());
                reply.put("wordCount", interimText.trim().isEmpty() ? 0 : interimText.trim().split("\\s+").length);
                reply.put("duration", duration);

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(reply)));

            } else if ("SAVE_FINAL".equalsIgnoreCase(action)) {
                // Client requests saving final transcription to MySQL
                String content = json.path("content").asText("");
                String title = json.path("title").asText("");
                String language = json.path("language").asText("Hinglish (Hindi + English)");
                double confidence = json.path("confidence").asDouble(0.96);
                double duration = json.path("duration").asDouble(0.0);
                String audioData = json.path("audioData").asText(null);
                String tags = json.path("tags").asText(null);

                TranscriptionRequest req = new TranscriptionRequest(
                        title, content, language, confidence, duration, audioData, tags
                );

                TranscriptionResponse saved = transcriptionService.createTranscription(req);

                Map<String, Object> reply = new HashMap<>();
                reply.put("type", "TRANSCRIPTION_SAVED");
                reply.put("savedRecord", saved);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(reply)));

            } else if ("PING".equalsIgnoreCase(action)) {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            Map<String, Object> error = new HashMap<>();
            error.put("type", "ERROR");
            error.put("message", e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeSessions.remove(session.getId());
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error on session {}: {}", session.getId(), exception.getMessage());
    }
}
