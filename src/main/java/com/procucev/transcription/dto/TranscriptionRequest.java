package com.procucev.transcription.dto;

import jakarta.validation.constraints.NotBlank;

public class TranscriptionRequest {

    private String title;

    @NotBlank(message = "Transcription content cannot be empty")
    private String content;

    private String detectedLanguage;
    private Double confidence;
    private Double durationSeconds;
    private String audioData;
    private String englishTranslation;
    private String tags;

    public TranscriptionRequest() {}

    public TranscriptionRequest(String title, String content, String detectedLanguage, Double confidence, Double durationSeconds, String audioData, String tags) {
        this.title = title;
        this.content = content;
        this.detectedLanguage = detectedLanguage;
        this.confidence = confidence;
        this.durationSeconds = durationSeconds;
        this.audioData = audioData;
        this.tags = tags;
    }

    public TranscriptionRequest(String title, String content, String detectedLanguage, Double confidence, Double durationSeconds, String audioData, String englishTranslation, String tags) {
        this.title = title;
        this.content = content;
        this.detectedLanguage = detectedLanguage;
        this.confidence = confidence;
        this.durationSeconds = durationSeconds;
        this.audioData = audioData;
        this.englishTranslation = englishTranslation;
        this.tags = tags;
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

    public String getAudioData() {
        return audioData;
    }

    public void setAudioData(String audioData) {
        this.audioData = audioData;
    }

    public String getEnglishTranslation() {
        return englishTranslation;
    }

    public void setEnglishTranslation(String englishTranslation) {
        this.englishTranslation = englishTranslation;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
