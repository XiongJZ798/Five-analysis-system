package com._5ganalysisrate.g5rate.repository;

import com._5ganalysisrate.g5rate.domain.entity.AiAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, Long> {

    Optional<AiAnalysisResult> findByTaskId(Long taskId);
}
