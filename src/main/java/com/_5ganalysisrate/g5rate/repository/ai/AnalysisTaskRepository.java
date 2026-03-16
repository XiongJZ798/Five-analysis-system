package com._5ganalysisrate.g5rate.repository.ai;

import com._5ganalysisrate.g5rate.model.ai.AnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {
    List<AnalysisTask> findByFileNameOrderByCreatedAtDesc(String fileName);
    List<AnalysisTask> findByStatusOrderByCreatedAtDesc(AnalysisTask.TaskStatus status);
}
