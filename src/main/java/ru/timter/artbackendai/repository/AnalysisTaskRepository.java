package ru.timter.artbackendai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.timter.artbackendai.entity.AnalysisTask;

import java.util.List;
import java.util.UUID;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, UUID> {
    List<AnalysisTask> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
