package com._5ganalysisrate.g5rate.repository;

import com._5ganalysisrate.g5rate.domain.entity.RuleAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RuleAnalysisResultRepository extends JpaRepository<RuleAnalysisResult, Long> {

    Optional<RuleAnalysisResult> findByTaskId(Long taskId);
}
