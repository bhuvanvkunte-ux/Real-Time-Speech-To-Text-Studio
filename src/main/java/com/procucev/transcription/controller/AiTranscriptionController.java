package com.procucev.transcription.controller;

import com.procucev.transcription.dto.LanguageDetectionResponse;
import com.procucev.transcription.service.AiTranscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiTranscriptionController {

    private final AiTranscriptionService aiService;

    public AiTranscriptionController(AiTranscriptionService aiService) {
        this.aiService = aiService;
    }

    /**
     * Linguistic analysis endpoint to detect Hindi, English, or Hinglish mixture
     */
    @PostMapping("/detect-language")
    public ResponseEntity<LanguageDetectionResponse> detectLanguage(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        return ResponseEntity.ok(aiService.detectLanguage(text));
    }

    /**
     * Gemini Live / Generative AI STT transcription of audio
     */
    @PostMapping("/transcribe-audio")
    public ResponseEntity<Map<String, Object>> transcribeAudio(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Gemini-Key", required = false) String customKey
    ) {
        String base64Audio = payload.get("audio");
        String mimeType = payload.getOrDefault("mimeType", "audio/webm");

        if (base64Audio == null || base64Audio.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Audio base64 data is required"));
        }

        Map<String, Object> result = aiService.transcribeWithGemini(base64Audio, mimeType, customKey);
        return ResponseEntity.ok(result);
    }

    /**
     * Refine and punctuate transcribed speech
     */
    @PostMapping("/refine")
    public ResponseEntity<Map<String, String>> refineTranscript(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Gemini-Key", required = false) String customKey
    ) {
        String text = payload.get("text");
        String refined = aiService.refineTranscript(text, customKey);
        return ResponseEntity.ok(Collections.singletonMap("refinedText", refined));
    }
}
