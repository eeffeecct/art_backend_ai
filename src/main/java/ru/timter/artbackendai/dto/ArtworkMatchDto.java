package ru.timter.artbackendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.timter.artbackendai.entity.Artwork;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkMatchDto {
    private Artwork artwork;
    private Double similarityScore;
    private String similarityPercentage; // e.g. "95.2%"
}
