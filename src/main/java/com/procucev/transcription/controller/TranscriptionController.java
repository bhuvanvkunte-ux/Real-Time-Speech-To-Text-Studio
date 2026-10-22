package com.procucev.transcription.controller;

import com.procucev.transcription.dto.BulkDeleteRequest;
import com.procucev.transcription.dto.DashboardStats;
import com.procucev.transcription.dto.TranscriptionRequest;
import com.procucev.transcription.dto.TranscriptionResponse;
import com.procucev.transcription.service.TranscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transcriptions")
@CrossOrigin(origins = "*")
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    /**
     * Create a new transcription (CRUD: Create)
     */
    @PostMapping
    public ResponseEntity<TranscriptionResponse> createTranscription(@Valid @RequestBody TranscriptionRequest request) {
        TranscriptionResponse created = transcriptionService.createTranscription(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Get paginated transcriptions with search and language filter (CRUD: Read)
     */
    @GetMapping
    public ResponseEntity<Page<TranscriptionResponse>> getTranscriptions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Page<TranscriptionResponse> result = transcriptionService.getTranscriptions(search, language, page, size, sortBy, direction);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all transcriptions as a plain list
     */
    @GetMapping("/all")
    public ResponseEntity<List<TranscriptionResponse>> getAllTranscriptions() {
        return ResponseEntity.ok(transcriptionService.getAllTranscriptionsList());
    }

    /**
     * Get single transcription by ID (CRUD: Read)
     */
    @GetMapping("/{id}")
    public ResponseEntity<TranscriptionResponse> getTranscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(transcriptionService.getTranscriptionById(id));
    }

    /**
     * Update transcription by ID (CRUD: Update)
     */
    @PutMapping("/{id}")
    public ResponseEntity<TranscriptionResponse> updateTranscription(
            @PathVariable Long id,
            @Valid @RequestBody TranscriptionRequest request
    ) {
        return ResponseEntity.ok(transcriptionService.updateTranscription(id, request));
    }

    /**
     * Delete transcription by ID (CRUD: Delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTranscription(@PathVariable Long id) {
        transcriptionService.deleteTranscription(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Transcription deleted successfully"));
    }

    /**
     * Bulk delete transcriptions (CRUD: Delete)
     */
    @PostMapping("/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDelete(@RequestBody BulkDeleteRequest request) {
        int deletedCount = transcriptionService.bulkDelete(request);
        return ResponseEntity.ok(Collections.singletonMap("deletedCount", deletedCount));
    }

    /**
     * Get dashboard analytics and multilingual metrics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(transcriptionService.getDashboardStats());
    }

    /**
     * Export transcriptions as CSV, JSON, or TXT
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTranscriptions(@RequestParam(defaultValue = "csv") String format) {
        byte[] data = transcriptionService.exportTranscriptions(format);

        String filename = "transcriptions-" + System.currentTimeMillis();
        MediaType mediaType;

        if ("csv".equalsIgnoreCase(format)) {
            filename += ".csv";
            mediaType = MediaType.parseMediaType("text/csv");
        } else if ("txt".equalsIgnoreCase(format)) {
            filename += ".txt";
            mediaType = MediaType.TEXT_PLAIN;
        } else {
            filename += ".json";
            mediaType = MediaType.APPLICATION_JSON;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }
}
