package ru.timter.artbackendai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timter.artbackendai.dto.AnalysisResultMessage;
import ru.timter.artbackendai.entity.AnalysisTask;
import ru.timter.artbackendai.entity.Artwork;
import ru.timter.artbackendai.entity.TaskStatus;
import ru.timter.artbackendai.repository.AnalysisTaskRepository;
import ru.timter.artbackendai.repository.ArtworkMatchProjection;
import ru.timter.artbackendai.repository.ArtworkRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultConsumer {

    private final AnalysisTaskRepository taskRepository;
    private final ArtworkRepository artworkRepository;

    @RabbitListener(queues = "${art.results.queue}")
    @Transactional
    public void handleResult(AnalysisResultMessage result) {
        log.info("Received result for task: {}", result.getTaskId());

        AnalysisTask task = taskRepository.findById(result.getTaskId())
                .orElse(null);

        if (task == null) {
            log.error("Task not found for result: {}", result.getTaskId());
            return;
        }

        // The worker explicitly reported a failure — mark the task FAILED so the
        // frontend stops polling instead of spinning forever.
        if ("FAILED".equalsIgnoreCase(result.getStatus())) {
            log.warn("Worker reported FAILED for task {}: {}", result.getTaskId(), result.getError());
            task.setStatus(TaskStatus.FAILED);
            taskRepository.save(task);
            return;
        }

        // Validate the embedding contract (CLIP-large => 768 dims) before touching pgvector,
        // so a malformed/partial result fails the task cleanly instead of throwing a cryptic
        // SQL cast error.
        if (result.getEmbedding() == null || result.getEmbedding().size() != 768) {
            log.error("Task {} has invalid embedding (size={}); marking FAILED",
                    result.getTaskId(),
                    result.getEmbedding() == null ? "null" : result.getEmbedding().size());
            task.setStatus(TaskStatus.FAILED);
            taskRepository.save(task);
            return;
        }

        try {
            // 1. Convert embedding to string format for pgvector
            String embeddingString = "[" + result.getEmbedding().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")) + "]";

            // 2. Find similar artworks
            List<ArtworkMatchProjection> similarArtworks = artworkRepository.findTop6SimilarWithScore(embeddingString);
            List<Integer> matches = similarArtworks.stream()
                    .map(a -> a.getId().intValue())
                    .collect(Collectors.toList());

            // 3. Update task
            task.setPalette(result.getPalette());
            task.setTags(result.getTags());
            task.setStyleBreakdown(result.getStyleBreakdown());
            task.setMatches(matches);
            task.setEmbedding(result.getEmbedding());
            task.setStatus(TaskStatus.COMPLETED);

            taskRepository.save(task);
            log.info("Task {} completed successfully", result.getTaskId());

        } catch (Exception e) {
            log.error("Error processing result for task {}: {}", result.getTaskId(), e.getMessage(), e);
            task.setStatus(TaskStatus.FAILED);
            taskRepository.save(task);
        }
    }
}
