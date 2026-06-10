package ru.timter.artbackendai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.timter.artbackendai.dto.AnalysisTaskMessage;
import ru.timter.artbackendai.dto.AnalysisTaskDto;
import ru.timter.artbackendai.entity.AnalysisTask;
import ru.timter.artbackendai.entity.TaskStatus;
import ru.timter.artbackendai.repository.AnalysisTaskRepository;

import ru.timter.artbackendai.dto.ArtworkMatchDto;
import ru.timter.artbackendai.repository.ArtworkMatchProjection;
import ru.timter.artbackendai.entity.Artwork;
import ru.timter.artbackendai.repository.ArtworkRepository;
import java.util.Locale;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class  AnalysisService {

    private final AnalysisTaskRepository taskRepository;
    private final ArtworkRepository artworkRepository;
    private final S3Service s3Service;
    private final RabbitTemplate rabbitTemplate;

    @Value("${art.exchange}")
    private String exchange;

    @Value("${art.analysis.routing-key}")
    private String routingKey;

    public AnalysisTask startAnalysis(UUID userId, MultipartFile file) throws IOException {
        validateFile(file);
        UUID taskId = UUID.randomUUID();
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String s3Key = taskId + fileExtension;

        log.info("Uploading file to S3: {}", s3Key);
        String savedKey = s3Service.uploadFile(s3Key, file.getInputStream(), file.getSize(), file.getContentType());
        String publicUrl = s3Service.getPublicUrl(savedKey);
        // Worker downloads via the INTERNAL MinIO endpoint (reachable inside the Docker network).
        String presignedUrl = s3Service.generateWorkerUrl(savedKey);

        AnalysisTask task = AnalysisTask.builder()
                .id(taskId)
                .userId(userId)
                .imageS3Url(publicUrl)
                .status(TaskStatus.PROCESSING)
                .build();

        taskRepository.save(task); // save to DB

        AnalysisTaskMessage message = AnalysisTaskMessage.builder()
                .taskId(taskId)
                .imageUrl(presignedUrl)
                .mode("auto")
                .build();

        log.info("Sending task to RabbitMQ: {}", taskId);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);

        return task;
    }

    public AnalysisTask getRawTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
    }

    /**
     * Fetch a task only if it belongs to the given user; otherwise behave as "not found"
     * (404) to avoid leaking the existence of other users' tasks.
     */
    private AnalysisTask getOwnedTask(UUID taskId, UUID userId) {
        AnalysisTask task = getRawTask(taskId);
        if (!task.getUserId().equals(userId)) {
            throw new RuntimeException("Task not found: " + taskId);
        }
        return task;
    }

    public List<AnalysisTask> getHistory(UUID userId) {
        return taskRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    public AnalysisTaskDto getTaskStatus(UUID taskId, UUID userId) {
        AnalysisTask task = getOwnedTask(taskId, userId);
        
        // Extract key and generate fresh presigned URL for the uploaded image
        String rawUrl = task.getImageS3Url();
        String objectKey = rawUrl;
        if (rawUrl.contains("/uploads/")) {
            objectKey = rawUrl.substring(rawUrl.indexOf("/uploads/") + 9);
        }
        String presignedImageUrl = s3Service.generatePresignedUrl(objectKey);
        
        AnalysisTaskDto dto = AnalysisTaskDto.builder()
                .id(task.getId())
                .imageS3Url(presignedImageUrl)
                .status(task.getStatus())
                .palette(task.getPalette())
                .tags(getDynamicTags(task))
                .styleBreakdown(task.getStyleBreakdown())
                .createdAt(task.getCreatedAt())
                .build();
                
        if (task.getStatus() == TaskStatus.COMPLETED) {
            dto.setMatches(fetchMatches(task, 6, 0));
        }
        
        return dto;
    }

    private List<String> getDynamicTags(AnalysisTask task) {
        if (task.getTags() != null && !task.getTags().isEmpty()) {
            return task.getTags();
        }
        
        if (task.getStyleBreakdown() == null || task.getStyleBreakdown().isEmpty()) {
            return List.of("Обработка...");
        }

        String primaryStyle = task.getStyleBreakdown().get(0).getStyle();
        
        return switch (primaryStyle) {
            case "Minimalism", "Минимализм" -> List.of("Лаконичность", "Чистота форм", "Строгость");
            case "Baroque", "Барокко" -> List.of("Динамизм", "Пышность", "Контраст света и тени");
            case "Impressionism", "Импрессионизм" -> List.of("Мимолетность", "Световоздушная среда", "Размытые контуры");
            case "Expressionism", "Экспрессионизм" -> List.of("Эмоциональность", "Искажение форм", "Яркие цвета");
            case "Cubism", "Кубизм" -> List.of("Геометризация", "Множественность точек зрения", "Плоскостность");
            case "Surrealism", "Сюрреализм" -> List.of("Алогизм", "Сновидность", "Совмещение несовместимого");
            case "Realism", "Реализм" -> List.of("Объективность", "Детализация", "Правдивость");
            case "Romanticism", "Романтизм" -> List.of("Духовность", "Культ чувств", "Драматизм");
            case "Pop_Art", "Поп-арт" -> List.of("Ирония", "Тиражируемость", "Яркие контуры");
            case "Ukiyo_e", "Укиё-э" -> List.of("Графичность", "Плоскостная композиция", "Линейный рисунок");
            case "Symbolism", "Символизм" -> List.of("Загадочность", "Иносказательность", "Метафоричность");
            case "Art_Nouveau_Modern", "Модерн (Ар-нуво)" -> List.of("Декоративность", "Плавные линии", "Орнаментальность");
            default -> List.of("Уникальный стиль", "Авторская манера");
        };
    }

    public List<ArtworkMatchDto> getMoreMatches(UUID taskId, int limit, int offset, UUID userId) {
        AnalysisTask task = getOwnedTask(taskId, userId);
        return fetchMatches(task, limit, offset);
    }

    private List<ArtworkMatchDto> fetchMatches(AnalysisTask task, int limit, int offset) {
        String embeddingString = task.getEmbeddingAsPgVector();

        if (embeddingString == null) {
            throw new RuntimeException("Task not yet completed or embedding not saved");
        }

        List<ArtworkMatchProjection> projections = artworkRepository.findSimilarPaginatedWithScore(embeddingString, limit, offset);

        return projections.stream().map(p -> {
            // Extract the key from the stored URL. Usually it looks like "http://.../dataset/author/image.jpg"
            // or just "author/image.jpg" depending on how it was imported.
            String rawUrl = p.getImageS3Url();
            String objectKey = rawUrl;
            
            // Try to extract just the key if it's a full URL
            if (rawUrl.contains("/dataset/")) {
                objectKey = rawUrl.substring(rawUrl.indexOf("/dataset/") + 9);
            }
            
            String presignedImageUrl = s3Service.generatePresignedUrlForDataset(objectKey);

            Artwork artwork = Artwork.builder()
                    .id(p.getId())
                    .artist(p.getArtist())
                    .title(p.getTitle())
                    .style(p.getStyle())
                    .imageS3Url(presignedImageUrl)
                    .build();
                    
            return ArtworkMatchDto.builder()
                    .artwork(artwork)
                    .similarityScore(p.getScore())
                    .similarityPercentage(String.format(Locale.US, "%.1f%%", p.getScore() * 100))
                    .build();
        }).collect(Collectors.toList());
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }
    }
}
