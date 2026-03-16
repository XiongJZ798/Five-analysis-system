package com._5ganalysisrate.g5rate.repository.ai;

import com._5ganalysisrate.g5rate.model.ai.AnalysisResultAi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisResultAiRepository extends JpaRepository<AnalysisResultAi, Long> {
    Optional<AnalysisResultAi> findByTaskId(Long taskId);
}
