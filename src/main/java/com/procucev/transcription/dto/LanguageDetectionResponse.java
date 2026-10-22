package com.procucev.transcription.dto;

import java.util.List;

public class LanguageDetectionResponse {

    private String primaryLanguage; // "Hinglish (Hindi + English)", "English", "Hindi"
    private double englishScore;
    private double hindiScore;
    private boolean isMultilingual;
    private List<String> detectedHindiTokens;
    private List<String> detectedEnglishTokens;

    public LanguageDetectionResponse() {}

    public LanguageDetectionResponse(String primaryLanguage, double englishScore, double hindiScore, 
                                     boolean isMultilingual, List<String> detectedHindiTokens, List<String> detectedEnglishTokens) {
        this.primaryLanguage = primaryLanguage;
        this.englishScore = englishScore;
        this.hindiScore = hindiScore;
        this.isMultilingual = isMultilingual;
        this.detectedHindiTokens = detectedHindiTokens;
        this.detectedEnglishTokens = detectedEnglishTokens;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public double getEnglishScore() {
        return englishScore;
    }

    public void setEnglishScore(double englishScore) {
        this.englishScore = englishScore;
    }

    public double getHindiScore() {
        return hindiScore;
    }

    public void setHindiScore(double hindiScore) {
        this.hindiScore = hindiScore;
    }

    public boolean isMultilingual() {
        return isMultilingual;
    }

    public void setMultilingual(boolean isMultilingual) {
        this.isMultilingual = isMultilingual;
    }

    public List<String> getDetectedHindiTokens() {
        return detectedHindiTokens;
    }

    public void setDetectedHindiTokens(List<String> detectedHindiTokens) {
        this.detectedHindiTokens = detectedHindiTokens;
    }

    public List<String> getDetectedEnglishTokens() {
        return detectedEnglishTokens;
    }

    public void setDetectedEnglishTokens(List<String> detectedEnglishTokens) {
        this.detectedEnglishTokens = detectedEnglishTokens;
    }
}
