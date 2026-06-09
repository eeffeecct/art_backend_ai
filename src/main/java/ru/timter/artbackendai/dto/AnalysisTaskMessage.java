package ru.timter.artbackendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisTaskMessage {
    private UUID taskId;
    private String imageUrl;
    private String mode;
}
