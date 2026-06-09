package ru.timter.artbackendai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultMessage {
    private UUID taskId;
    private List<Double> embedding;
    private List<String> palette;
    private List<String> tags;
    private List<StyleProbability> styleBreakdown;
}
