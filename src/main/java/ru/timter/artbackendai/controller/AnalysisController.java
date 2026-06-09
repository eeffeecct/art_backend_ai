package ru.timter.artbackendai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.timter.artbackendai.dto.AnalysisTaskDto;
import ru.timter.artbackendai.dto.ArtworkMatchDto;
import ru.timter.artbackendai.entity.AnalysisTask;
import ru.timter.artbackendai.service.AnalysisService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/art")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisTask> analyze(@RequestParam("file") MultipartFile file) throws IOException {
        UUID mockUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        AnalysisTask task = analysisService.startAnalysis(mockUserId, file);
        return ResponseEntity.accepted().body(task);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<AnalysisTaskDto> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(analysisService.getTaskStatus(id));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisTask>> getHistory() {
        UUID mockUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        return ResponseEntity.ok(analysisService.getHistory(mockUserId));
    }

    @GetMapping("/tasks/{id}/more")
    public ResponseEntity<List<ArtworkMatchDto>> getMore(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(defaultValue = "6") int offset) {
        return ResponseEntity.ok(analysisService.getMoreMatches(id, limit, offset));
    }
}
