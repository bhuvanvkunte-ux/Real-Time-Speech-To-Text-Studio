package com.procucev.transcription.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcriptions")
public class Transcription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "detected_language", length = 100)
    private String detectedLanguage = "Hinglish (Hindi + English)";

    @Column(name = "confidence")
    private Double confidence = 0.95;

    @Column(name = "duration_seconds")
    private Double durationSeconds = 0.0;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    @Column(name = "audio_data", columnDefinition = "LONGTEXT")
    private String audioData;

    @Column(length = 255)
    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.wordCount == null && this.content != null) {
            this.wordCount = calculateWordCount(this.content);
        }
        if (this.title == null || this.title.trim().isEmpty()) {
            this.title = generateTitleFromContent(this.content);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.content != null) {
            this.wordCount = calculateWordCount(this.content);
        }
    }

    public static int calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    public static String generateTitleFromContent(String text) {
        if (text == null || text.trim().isEmpty()) return "Voice Transcription";
        String trimmed = text.trim();
        int wordsLimit = 6;
        String[] words = trimmed.split("\\s+");
        if (words.length <= wordsLimit) {
            return trimmed;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordsLimit; i++) {
            sb.append(words[i]).append(" ");
        }
        return sb.toString().trim() + "...";
    }

    // Default Constructor
    public Transcription() {}

    // Parameterized Constructor
    public Transcription(String title, String content, String detectedLanguage, Double confidence, Double durationSeconds, String audioData, String tags) {
        this.title = title;
        this.content = content;
        this.detectedLanguage = detectedLanguage;
        this.confidence = confidence;
        this.durationSeconds = durationSeconds;
        this.audioData = audioData;
        this.tags = tags;
        this.wordCount = calculateWordCount(content);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.wordCount = calculateWordCount(content);
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(String detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getAudioData() {
        return audioData;
    }

    public void setAudioData(String audioData) {
        this.audioData = audioData;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
