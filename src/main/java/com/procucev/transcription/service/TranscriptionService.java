package com.procucev.transcription.service;

import com.procucev.transcription.dto.BulkDeleteRequest;
import com.procucev.transcription.dto.DashboardStats;
import com.procucev.transcription.dto.TranscriptionRequest;
import com.procucev.transcription.dto.TranscriptionResponse;
import com.procucev.transcription.model.Transcription;
import com.procucev.transcription.repository.TranscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TranscriptionService {

    private final TranscriptionRepository repository;
    private final AiTranscriptionService aiService;

    public TranscriptionService(TranscriptionRepository repository, AiTranscriptionService aiService) {
        this.repository = repository;
        this.aiService = aiService;
    }

    public TranscriptionResponse createTranscription(TranscriptionRequest request) {
        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Transcription content cannot be empty.");
        }

        // Convert any Devanagari Hindi script to English/Roman writing text
        content = aiService.toRomanScript(content);

        // Auto-detect language if not explicitly provided or generic
        String detectedLang = request.getDetectedLanguage();
        if (detectedLang == null || detectedLang.trim().isEmpty() || detectedLang.equalsIgnoreCase("Auto")) {
            detectedLang = aiService.detectLanguage(content).getPrimaryLanguage();
        }

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = Transcription.generateTitleFromContent(content);
        }

        Double confidence = request.getConfidence() != null ? request.getConfidence() : 0.95;
        Double duration = request.getDurationSeconds() != null ? request.getDurationSeconds() : 0.0;
        String tags = request.getTags();
        if (tags == null || tags.trim().isEmpty()) {
            tags = detectedLang.contains("Hinglish") ? "Hinglish, Multilingual" : detectedLang;
        }

        String translation = request.getEnglishTranslation();
        if (translation == null || translation.trim().isEmpty()) {
            translation = aiService.translateToEnglish(content, null);
        }

        Transcription entity = new Transcription(
                title.trim(),
                content,
                detectedLang,
                confidence,
                duration,
                request.getAudioData(),
                tags
        );
        entity.setEnglishTranslation(translation);

        Transcription saved = repository.save(entity);
        return TranscriptionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<TranscriptionResponse> getTranscriptions(String search, String language, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String langParam = (language != null && !language.trim().isEmpty() && !language.equalsIgnoreCase("all")) ? language.trim() : null;

        Page<Transcription> result = repository.searchTranscriptions(searchParam, langParam, pageable);
        return result.map(TranscriptionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<TranscriptionResponse> getAllTranscriptionsList() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(TranscriptionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TranscriptionResponse getTranscriptionById(Long id) {
        Transcription transcription = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transcription not found with ID: " + id));
        return TranscriptionResponse.fromEntity(transcription);
    }

    public TranscriptionResponse updateTranscription(Long id, TranscriptionRequest request) {
        Transcription existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transcription not found with ID: " + id));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            existing.setTitle(request.getTitle().trim());
        }

        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            existing.setContent(request.getContent().trim());
        }

        if (request.getDetectedLanguage() != null && !request.getDetectedLanguage().trim().isEmpty()) {
            existing.setDetectedLanguage(request.getDetectedLanguage().trim());
        }

        if (request.getConfidence() != null) {
            existing.setConfidence(request.getConfidence());
        }

        if (request.getDurationSeconds() != null) {
            existing.setDurationSeconds(request.getDurationSeconds());
        }

        if (request.getTags() != null) {
            existing.setTags(request.getTags().trim());
        }

        if (request.getAudioData() != null) {
            existing.setAudioData(request.getAudioData());
        }

        if (request.getEnglishTranslation() != null) {
            existing.setEnglishTranslation(request.getEnglishTranslation().trim());
        }

        Transcription updated = repository.save(existing);
        return TranscriptionResponse.fromEntity(updated);
    }

    public void deleteTranscription(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Transcription not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    public int bulkDelete(BulkDeleteRequest bulkRequest) {
        if (bulkRequest.getIds() == null || bulkRequest.getIds().isEmpty()) {
            return 0;
        }
        List<Transcription> toDelete = repository.findAllById(bulkRequest.getIds());
        repository.deleteAll(toDelete);
        return toDelete.size();
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        long totalTranscriptions = repository.count();
        long totalWords = repository.sumTotalWordCount();
        double totalDuration = repository.sumTotalDurationSeconds();
        long hinglishCount = repository.countHinglishTranscriptions();

        double hinglishPercentage = totalTranscriptions > 0 ? ((double) hinglishCount / totalTranscriptions) * 100.0 : 0.0;
        hinglishPercentage = Math.round(hinglishPercentage * 10.0) / 10.0;

        Map<String, Long> breakdown = new LinkedHashMap<>();
        List<Object[]> rows = repository.countGroupByLanguage();
        for (Object[] row : rows) {
            String lang = row[0] != null ? (String) row[0] : "Unknown";
            Long count = ((Number) row[1]).longValue();
            breakdown.put(lang, count);
        }

        return new DashboardStats(
                totalTranscriptions,
                totalWords,
                Math.round(totalDuration * 10.0) / 10.0,
                hinglishCount,
                hinglishPercentage,
                breakdown
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportTranscriptions(String format) {
        List<Transcription> list = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("ID,Title,Content,Language,Confidence,Duration(s),WordCount,Tags,CreatedAt\n");
            for (Transcription t : list) {
                sb.append(t.getId()).append(",")
                  .append("\"").append(escapeCsv(t.getTitle())).append("\",")
                  .append("\"").append(escapeCsv(t.getContent())).append("\",")
                  .append("\"").append(escapeCsv(t.getDetectedLanguage())).append("\",")
                  .append(t.getConfidence()).append(",")
                  .append(t.getDurationSeconds()).append(",")
                  .append(t.getWordCount()).append(",")
                  .append("\"").append(escapeCsv(t.getTags() != null ? t.getTags() : "")).append("\",")
                  .append(t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "")
                  .append("\n");
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } else if ("txt".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== PROCUCEV TRANSCRIPTION EXPORT ===\n\n");
            for (Transcription t : list) {
                sb.append("ID: ").append(t.getId()).append("\n");
                sb.append("Title: ").append(t.getTitle()).append("\n");
                sb.append("Date: ").append(t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "").append("\n");
                sb.append("Language: ").append(t.getDetectedLanguage()).append("\n");
                sb.append("Duration: ").append(t.getDurationSeconds()).append("s | Words: ").append(t.getWordCount()).append("\n");
                sb.append("Transcription:\n").append(t.getContent()).append("\n");
                sb.append("------------------------------------------------------------\n\n");
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            // Default JSON
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                Transcription t = list.get(i);
                sb.append("  {\n");
                sb.append("    \"id\": ").append(t.getId()).append(",\n");
                sb.append("    \"title\": \"").append(escapeJson(t.getTitle())).append("\",\n");
                sb.append("    \"content\": \"").append(escapeJson(t.getContent())).append("\",\n");
                sb.append("    \"detectedLanguage\": \"").append(escapeJson(t.getDetectedLanguage())).append("\",\n");
                sb.append("    \"confidence\": ").append(t.getConfidence()).append(",\n");
                sb.append("    \"durationSeconds\": ").append(t.getDurationSeconds()).append(",\n");
                sb.append("    \"wordCount\": ").append(t.getWordCount()).append(",\n");
                sb.append("    \"tags\": \"").append(escapeJson(t.getTags() != null ? t.getTags() : "")).append("\",\n");
                sb.append("    \"createdAt\": \"").append(t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "").append("\"\n");
                sb.append("  }").append(i < list.size() - 1 ? "," : "").append("\n");
            }
            sb.append("]\n");
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
