package com._5ganalysisrate.g5rate.repository;

import com._5ganalysisrate.g5rate.domain.entity.AnalysisTask;
import com._5ganalysisrate.g5rate.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {

    List<AnalysisTask> findByUploadFileId(String uploadFileId);

    List<AnalysisTask> findByStatus(TaskStatus status);
}
