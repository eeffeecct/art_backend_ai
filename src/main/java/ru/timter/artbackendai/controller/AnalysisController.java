package ru.timter.artbackendai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.timter.artbackendai.dto.AnalysisTaskDto;
import ru.timter.artbackendai.dto.ArtworkMatchDto;
import ru.timter.artbackendai.entity.AnalysisTask;
import ru.timter.artbackendai.security.UserPrincipal;
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
    public ResponseEntity<AnalysisTask> analyze(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        AnalysisTask task = analysisService.startAnalysis(principal.getId(), file);
        return ResponseEntity.accepted().body(task);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<AnalysisTaskDto> getTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analysisService.getTaskStatus(id, principal.getId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisTask>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analysisService.getHistory(principal.getId()));
    }

    @GetMapping("/tasks/{id}/more")
    public ResponseEntity<List<ArtworkMatchDto>> getMore(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analysisService.getMoreMatches(id, limit, offset, principal.getId()));
    }
}
