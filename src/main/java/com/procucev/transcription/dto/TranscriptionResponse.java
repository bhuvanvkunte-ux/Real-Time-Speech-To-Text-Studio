package com.procucev.transcription.dto;

import com.procucev.transcription.model.Transcription;
import java.time.LocalDateTime;

public class TranscriptionResponse {

    private Long id;
    private String title;
    private String content;
    private String detectedLanguage;
    private Double confidence;
    private Double durationSeconds;
    private Integer wordCount;
    private String audioData;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TranscriptionResponse() {}

    public static TranscriptionResponse fromEntity(Transcription entity) {
        TranscriptionResponse resp = new TranscriptionResponse();
        resp.setId(entity.getId());
        resp.setTitle(entity.getTitle());
        resp.setContent(entity.getContent());
        resp.setDetectedLanguage(entity.getDetectedLanguage());
        resp.setConfidence(entity.getConfidence());
        resp.setDurationSeconds(entity.getDurationSeconds());
        resp.setWordCount(entity.getWordCount());
        resp.setAudioData(entity.getAudioData());
        resp.setTags(entity.getTags());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }

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
