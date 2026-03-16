package com._5ganalysisrate.g5rate.repository;

import com._5ganalysisrate.g5rate.domain.entity.AnalysisFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisFeedbackRepository extends JpaRepository<AnalysisFeedback, Long> {

    List<AnalysisFeedback> findByTaskId(Long taskId);
}
