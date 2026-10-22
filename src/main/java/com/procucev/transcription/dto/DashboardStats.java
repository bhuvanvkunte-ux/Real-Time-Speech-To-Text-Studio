package com.procucev.transcription.dto;

import java.util.Map;

public class DashboardStats {

    private long totalTranscriptions;
    private long totalWords;
    private double totalDurationSeconds;
    private long hinglishCount;
    private double hinglishPercentage;
    private Map<String, Long> languageBreakdown;

    public DashboardStats() {}

    public DashboardStats(long totalTranscriptions, long totalWords, double totalDurationSeconds, 
                          long hinglishCount, double hinglishPercentage, Map<String, Long> languageBreakdown) {
        this.totalTranscriptions = totalTranscriptions;
        this.totalWords = totalWords;
        this.totalDurationSeconds = totalDurationSeconds;
        this.hinglishCount = hinglishCount;
        this.hinglishPercentage = hinglishPercentage;
        this.languageBreakdown = languageBreakdown;
    }

    public long getTotalTranscriptions() {
        return totalTranscriptions;
    }

    public void setTotalTranscriptions(long totalTranscriptions) {
        this.totalTranscriptions = totalTranscriptions;
    }

    public long getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(long totalWords) {
        this.totalWords = totalWords;
    }

    public double getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(double totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public long getHinglishCount() {
        return hinglishCount;
    }

    public void setHinglishCount(long hinglishCount) {
        this.hinglishCount = hinglishCount;
    }

    public double getHinglishPercentage() {
        return hinglishPercentage;
    }

    public void setHinglishPercentage(double hinglishPercentage) {
        this.hinglishPercentage = hinglishPercentage;
    }

    public Map<String, Long> getLanguageBreakdown() {
        return languageBreakdown;
    }

    public void setLanguageBreakdown(Map<String, Long> languageBreakdown) {
        this.languageBreakdown = languageBreakdown;
    }
}
