package com._5ganalysisrate.g5rate.repository;

import com._5ganalysisrate.g5rate.model.PeakRateHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeakRateHistoryRepository extends JpaRepository<PeakRateHistory, Long> {
    /**
     * 分页查询计算历史记录
     */
    Page<PeakRateHistory> findAllByOrderByCreateTimeDesc(Pageable pageable);
} 