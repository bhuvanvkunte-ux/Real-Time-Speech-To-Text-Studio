package com.procucev.transcription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procucev.transcription.dto.LanguageDetectionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class AiTranscriptionService {

    @Value("${gemini.api.key:}")
    private String configuredGeminiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Common Romanized Hindi / Hinglish indicator vocabulary
    private static final Set<String> HINGLISH_KEYWORDS = new HashSet<>(Arrays.asList(
            "hai", "hain", "ho", "tha", "thi", "the", "kya", "kyu", "kyun", "kaise", "kahan", "kab",
            "mera", "meri", "mere", "hum", "humara", "tum", "tumhara", "aap", "aapka", "unka", "inka",
            "karna", "karo", "karega", "karenge", "karte", "kar", "liya", "diya", "hoga", "hogi",
            "nahi", "mat", "haan", "theek", "accha", "bahut", "bhi", "toh", "lekin", "magar", "aur",
            "par", "se", "ko", "ke", "ki", "ka", "mein", "me", "pe", "tak", "saath", "baad", "pehle",
            "aaj", "kal", "abhi", "yahan", "wahan", "idhar", "udhar", "chahiye", "samajh", "batao",
            "bolo", "dekho", "suno", "jaise", "waise", "sab", "kuch", "koi", "kaam", "baat", "shuru",
            "khatam", "dost", "bhai", "yaar", "sir", "mam", "paas", "door", "raha", "rahi", "rahe"
    ));

    // Common English words
    private static final Set<String> ENGLISH_KEYWORDS = new HashSet<>(Arrays.asList(
            "the", "is", "are", "was", "were", "and", "or", "in", "on", "at", "to", "for", "with",
            "this", "that", "these", "those", "we", "you", "they", "he", "she", "it", "my", "your",
            "have", "has", "had", "do", "does", "did", "can", "could", "will", "would", "should",
            "meeting", "project", "work", "time", "team", "client", "code", "system", "service",
            "call", "status", "update", "today", "tomorrow", "task", "review", "feature", "build",
            "application", "deploy", "database", "api", "backend", "frontend", "hello", "thank", "thanks"
    ));

    public AiTranscriptionService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Detect whether the transcribed text is English, Hindi (Devanagari or Romanized),
     * or multilingual code-switching "Hinglish (Hindi + English)".
     */
    public LanguageDetectionResponse detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new LanguageDetectionResponse("English", 1.0, 0.0, false, Collections.emptyList(), Collections.emptyList());
        }

        String cleaned = text.trim();
        boolean hasDevanagari = Pattern.compile("[\\u0900-\\u097F]").matcher(cleaned).find();

        String[] tokens = cleaned.toLowerCase().replaceAll("[^a-z0-9\\u0900-\\u097F\\s]", " ").split("\\s+");
        List<String> hindiTokens = new ArrayList<>();
        List<String> englishTokens = new ArrayList<>();

        int totalWords = 0;
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            totalWords++;

            boolean isDevanagariWord = Pattern.compile("[\\u0900-\\u097F]").matcher(token).find();
            if (isDevanagariWord || HINGLISH_KEYWORDS.contains(token)) {
                hindiTokens.add(token);
            } else if (ENGLISH_KEYWORDS.contains(token)) {
                englishTokens.add(token);
            }
        }

        if (totalWords == 0) totalWords = 1;

        double hindiRatio = (double) hindiTokens.size() / totalWords;
        double englishRatio = (double) englishTokens.size() / totalWords;

        // If Devanagari script is present alongside English words, or if both English & Hindi tokens exist
        String primaryLanguage;
        boolean isMultilingual;

        if (hasDevanagari && !englishTokens.isEmpty()) {
            primaryLanguage = "Hinglish (Hindi + English)";
            isMultilingual = true;
        } else if (!hindiTokens.isEmpty() && !englishTokens.isEmpty()) {
            primaryLanguage = "Hinglish (Hindi + English)";
            isMultilingual = true;
        } else if (hasDevanagari || hindiRatio > 0.4) {
            primaryLanguage = "Hindi";
            isMultilingual = false;
        } else {
            primaryLanguage = "English";
            isMultilingual = false;
        }

        return new LanguageDetectionResponse(
                primaryLanguage,
                Math.round(englishRatio * 100.0) / 100.0,
                Math.round(hindiRatio * 100.0) / 100.0,
                isMultilingual,
                hindiTokens,
                englishTokens
        );
    }

    /**
     * Transcribe or refine audio/text using Gemini Live / Google Generative AI API.
     */
    public Map<String, Object> transcribeWithGemini(String base64Audio, String mimeType, String apiKeyOverride) {
        String apiKey = (apiKeyOverride != null && !apiKeyOverride.trim().isEmpty()) ? apiKeyOverride.trim() : configuredGeminiKey;

        Map<String, Object> response = new HashMap<>();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "No Gemini API key provided. Please configure GEMINI_API_KEY or provide it in the UI settings.");
            return response;
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            // Construct Gemini multimodal request payload
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType != null ? mimeType : "audio/webm");
            inlineData.put("data", base64Audio);

            Map<String, Object> partAudio = new HashMap<>();
            partAudio.put("inlineData", inlineData);

            Map<String, Object> partPrompt = new HashMap<>();
            partPrompt.put("text", "Transcribe this audio precisely. The audio contains Hindi, English, or a mixture of both (Hinglish). Provide the transcription written STRICTLY in ENGLISH / LATIN ALPHABET (Roman script Hinglish), NOT Devanagari script. Output ONLY the English-letter transcription without conversational prefix or quotes.");

            List<Map<String, Object>> parts = Arrays.asList(partAudio, partPrompt);
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("parts", parts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(contentMap));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> apiResponse = restTemplate.postForEntity(url, entity, String.class);

            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                JsonNode root = objectMapper.readTree(apiResponse.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    String transcribed = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    String romanized = toRomanScript(transcribed.trim());
                    LanguageDetectionResponse langInfo = detectLanguage(romanized);

                    response.put("success", true);
                    response.put("transcription", romanized);
                    response.put("detectedLanguage", langInfo.getPrimaryLanguage());
                    response.put("confidence", 0.98);
                    response.put("isMultilingual", langInfo.isMultilingual());
                    return response;
                }
            }

            response.put("success", false);
            response.put("error", "Unexpected response from Gemini API");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Gemini API error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Translate Hindi or Hinglish text into fluent English.
     */
    public String translateToEnglish(String text, String apiKeyOverride) {
        if (text == null || text.trim().isEmpty()) return "";

        String apiKey = (apiKeyOverride != null && !apiKeyOverride.trim().isEmpty()) ? apiKeyOverride.trim() : configuredGeminiKey;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
                String prompt = "Translate this Hindi or Hinglish sentence into natural, fluent English text. Output ONLY the English translation without quotes or explanations:\n\n" + text;

                Map<String, Object> part = Collections.singletonMap("text", prompt);
                Map<String, Object> content = Collections.singletonMap("parts", Collections.singletonList(part));
                Map<String, Object> body = Collections.singletonMap("contents", Collections.singletonList(content));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    JsonNode root = objectMapper.readTree(resp.getBody());
                    return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().trim();
                }
            } catch (Exception ignored) {
                // Fall back to Roman script
            }
        }

        return toRomanScript(text);
    }

    /**
     * Transliterates Devanagari text to English / Roman script.
     */
    public String toRomanScript(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        String res = text;
        // Common English loanwords written in Devanagari
        Map<String, String> commonWords = new LinkedHashMap<>();
        commonWords.put("हेलो", "Hello");
        commonWords.put("हेल्लो", "Hello");
        commonWords.put("माय", "my");
        commonWords.put("नेम", "name");
        commonWords.put("इस", "is");
        commonWords.put("थैंक", "thank");
        commonWords.put("थैंक्यू", "thank you");
        commonWords.put("प्लीज", "please");
        commonWords.put("ओके", "okay");
        commonWords.put("वेल", "well");
        commonWords.put("गुड", "good");
        commonWords.put("मॉर्निंग", "morning");
        commonWords.put("यस", "yes");
        commonWords.put("नो", "no");
        commonWords.put("सर", "sir");
        commonWords.put("मैम", "maam");
        commonWords.put("बॉस", "boss");
        commonWords.put("टीम", "team");
        commonWords.put("कॉल", "call");
        commonWords.put("मीटिंग", "meeting");
        commonWords.put("प्रोजेक्ट", "project");
        commonWords.put("अपडेट", "update");
        commonWords.put("कोड", "code");
        commonWords.put("सिस्टम", "system");

        for (Map.Entry<String, String> entry : commonWords.entrySet()) {
            res = res.replaceAll("(?i)\\b" + entry.getKey() + "\\b", entry.getValue());
        }

        if (!Pattern.compile("[\\u0900-\\u097F]").matcher(res).find()) {
            return res;
        }

        // Devanagari character by character phonetic transliteration
        StringBuilder sb = new StringBuilder();
        int len = res.length();
        for (int i = 0; i < len; i++) {
            char c = res.charAt(i);

            // Vowels
            switch (c) {
                case 'अ': sb.append("a"); continue;
                case 'आ': sb.append("aa"); continue;
                case 'इ': sb.append("i"); continue;
                case 'ई': sb.append("ee"); continue;
                case 'उ': sb.append("u"); continue;
                case 'ऊ': sb.append("oo"); continue;
                case 'ऋ': sb.append("ri"); continue;
                case 'ए': sb.append("e"); continue;
                case 'ऐ': sb.append("ai"); continue;
                case 'ओ': sb.append("o"); continue;
                case 'औ': sb.append("au"); continue;
                case 'ं': case 'ँ': sb.append("n"); continue;
                case 'ः': sb.append("h"); continue;
                // Matras
                case 'ा': sb.append("a"); continue;
                case 'ि': sb.append("i"); continue;
                case 'ी': sb.append("ee"); continue;
                case 'ु': sb.append("u"); continue;
                case 'ू': sb.append("oo"); continue;
                case 'ृ': sb.append("ri"); continue;
                case 'े': sb.append("e"); continue;
                case 'ै': sb.append("ai"); continue;
                case 'ो': sb.append("o"); continue;
                case 'ौ': sb.append("au"); continue;
                case '्': continue; // halant / virama
            }

            // Consonants
            String consonant = null;
            switch (c) {
                case 'क': consonant = "k"; break;
                case 'ख': consonant = "kh"; break;
                case 'ग': consonant = "g"; break;
                case 'घ': consonant = "gh"; break;
                case 'ङ': consonant = "ng"; break;
                case 'च': consonant = "ch"; break;
                case 'छ': consonant = "chh"; break;
                case 'ज': consonant = "j"; break;
                case 'झ': consonant = "jh"; break;
                case 'ञ': consonant = "ny"; break;
                case 'ट': consonant = "t"; break;
                case 'ठ': consonant = "th"; break;
                case 'ड': consonant = "d"; break;
                case 'ढ': consonant = "dh"; break;
                case 'ण': consonant = "n"; break;
                case 'त': consonant = "t"; break;
                case 'थ': consonant = "th"; break;
                case 'द': consonant = "d"; break;
                case 'ध': consonant = "dh"; break;
                case 'न': consonant = "n"; break;
                case 'प': consonant = "p"; break;
                case 'फ': consonant = "ph"; break;
                case 'ब': consonant = "b"; break;
                case 'भ': consonant = "bh"; break;
                case 'म': consonant = "m"; break;
                case 'य': consonant = "y"; break;
                case 'र': consonant = "r"; break;
                case 'ल': consonant = "l"; break;
                case 'व': consonant = "v"; break;
                case 'श': consonant = "sh"; break;
                case 'ष': consonant = "sh"; break;
                case 'स': consonant = "s"; break;
                case 'ह': consonant = "h"; break;
                case '\u0958': consonant = "q"; break;
                case '\u0959': consonant = "kh"; break;
                case '\u095A': consonant = "gh"; break;
                case '\u095B': consonant = "z"; break;
                case '\u095C': consonant = "r"; break;
                case '\u095D': consonant = "rh"; break;
                case '\u095E': consonant = "f"; break;
            }

            if (consonant != null) {
                sb.append(consonant);
                boolean hasNextMatra = false;
                if (i + 1 < len) {
                    char next = res.charAt(i + 1);
                    if ((next >= '\u093E' && next <= '\u094C') || next == '\u094D' || next == ' ' || Character.isWhitespace(next)) {
                        hasNextMatra = true;
                    }
                }
                if (!hasNextMatra && i + 1 < len && Character.isLetter(res.charAt(i + 1))) {
                    sb.append("a");
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Refine and punctuate multilingual transcript using Gemini or linguistic rules.
     */
    public String refineTranscript(String rawText, String apiKeyOverride) {
        if (rawText == null || rawText.trim().isEmpty()) return "";

        String apiKey = (apiKeyOverride != null && !apiKeyOverride.trim().isEmpty()) ? apiKeyOverride.trim() : configuredGeminiKey;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
                String prompt = "Format and punctuate this real-time multilingual transcription (mixture of Hindi and English). Output STRICTLY in ENGLISH ALPHABET / LATIN LETTERS (Roman Hinglish or English). Capitalize appropriately. Output ONLY the polished text:\n\n" + rawText;

                Map<String, Object> part = Collections.singletonMap("text", prompt);
                Map<String, Object> content = Collections.singletonMap("parts", Collections.singletonList(part));
                Map<String, Object> body = Collections.singletonMap("contents", Collections.singletonList(content));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    JsonNode root = objectMapper.readTree(resp.getBody());
                    return toRomanScript(root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().trim());
                }
            } catch (Exception ignored) {
                // Fall back to rule-based refinement
            }
        }

        // Rule-based punctuation and capitalization in English letters
        String trimmed = toRomanScript(rawText).trim();
        if (!trimmed.isEmpty()) {
            trimmed = Character.toUpperCase(trimmed.charAt(0)) + (trimmed.length() > 1 ? trimmed.substring(1) : "");
            if (!trimmed.endsWith(".") && !trimmed.endsWith("?") && !trimmed.endsWith("!")) {
                trimmed += ".";
            }
        }
        return trimmed;
    }
}
