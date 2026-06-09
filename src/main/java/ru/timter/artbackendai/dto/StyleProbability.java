package ru.timter.artbackendai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleProbability {
    private String style;
    private String prob; // String representation, e.g., "85.4%"
    private Double val;  // Numeric representation, e.g., 85.4
}

