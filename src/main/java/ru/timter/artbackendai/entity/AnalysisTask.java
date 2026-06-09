package ru.timter.artbackendai.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import ru.timter.artbackendai.dto.StyleProbability;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analysis_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisTask {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "image_s3_url", nullable = false)
    private String imageS3Url;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING) // ("PROCESSING", "COMPLETED")
    private TaskStatus status;

    @Type(ListArrayType.class)
    @Column(name = "palette", columnDefinition = "text[]")
    private List<String> palette; // list of colors in HEX.

    @Type(ListArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags; // Formal characteristics (e.g. "Strict", "Modern")

    @Type(JsonBinaryType.class)
    @Column(name = "style_breakdown", columnDefinition = "jsonb")
    private List<StyleProbability> styleBreakdown; // bin format for list of (style + probability)

    @Type(ListArrayType.class)
    @Column(name = "matches", columnDefinition = "integer[]")
    private List<Integer> matches; // Top-6 matches ID`s

    @Type(ListArrayType.class)
    @Column(name = "embedding", columnDefinition = "double precision[]")
    private List<Double> embedding; // picture in vector format stored as array

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getEmbeddingAsPgVector() {
        if (embedding == null || embedding.isEmpty()) {
            return null;
        }
        return "[" + embedding.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",")) + "]";
    }
}
