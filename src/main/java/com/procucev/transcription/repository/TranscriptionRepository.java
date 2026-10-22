package com.procucev.transcription.repository;

import com.procucev.transcription.model.Transcription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {

    // Keyword search across title and content
    @Query("SELECT t FROM Transcription t WHERE " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:language IS NULL OR LOWER(t.detectedLanguage) LIKE LOWER(CONCAT('%', :language, '%')))")
    Page<Transcription> searchTranscriptions(@Param("search") String search, 
                                            @Param("language") String language, 
                                            Pageable pageable);

    // Filter by detected language
    List<Transcription> findByDetectedLanguageContainingIgnoreCaseOrderByCreatedAtDesc(String language);

    // Top recent transcriptions
    List<Transcription> findTop10ByOrderByCreatedAtDesc();

    // Stats queries
    @Query("SELECT COALESCE(SUM(t.wordCount), 0) FROM Transcription t")
    Long sumTotalWordCount();

    @Query("SELECT COALESCE(SUM(t.durationSeconds), 0.0) FROM Transcription t")
    Double sumTotalDurationSeconds();

    @Query("SELECT COUNT(t) FROM Transcription t WHERE LOWER(t.detectedLanguage) LIKE '%hinglish%' OR (LOWER(t.detectedLanguage) LIKE '%hindi%' AND LOWER(t.detectedLanguage) LIKE '%english%')")
    Long countHinglishTranscriptions();

    @Query("SELECT t.detectedLanguage, COUNT(t) FROM Transcription t GROUP BY t.detectedLanguage")
    List<Object[]> countGroupByLanguage();
}
