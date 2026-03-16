package com._5ganalysisrate.g5rate.repository.ai;

import com._5ganalysisrate.g5rate.model.ai.AnalysisResultRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisResultRuleRepository extends JpaRepository<AnalysisResultRule, Long> {
    Optional<AnalysisResultRule> findByTaskId(Long taskId);
}
