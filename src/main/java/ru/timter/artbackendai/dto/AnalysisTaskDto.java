package ru.timter.artbackendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.timter.artbackendai.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisTaskDto {
    private UUID id;
    private String imageS3Url;
    private TaskStatus status;
    private List<String> palette;
    private List<String> tags;
    private List<StyleProbability> styleBreakdown;
    private List<ArtworkMatchDto> matches;
    private LocalDateTime createdAt;
}
