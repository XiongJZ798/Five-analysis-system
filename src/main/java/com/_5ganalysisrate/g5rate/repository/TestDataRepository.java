package com._5ganalysisrate.g5rate.repository;

import com._5ganalysisrate.g5rate.model.TestData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

//
@Repository
public interface TestDataRepository extends JpaRepository<TestData, Long> {
    
    /**
     * 查询指定时间范围内的测试数据
     */
    List<TestData> findByTestTimeBetweenOrderByTestTimeAsc(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计各速率区间的数据量
     */
    @Query("SELECT COUNT(t) FROM TestData t WHERE t.macThroughput >= ?1 AND t.macThroughput < ?2")
    Long countByMacThroughputRange(Double minRate, Double maxRate);
    
    /**
     * 统计总数据量
     */
    @Query("SELECT COUNT(t) FROM TestData t WHERE t.macThroughput IS NOT NULL")
    Long countTotalRecords();
    
    /**
     * 统计MAC吞吐量不为空的记录数
     */
    @Query("SELECT COUNT(t) FROM TestData t WHERE t.macThroughput IS NOT NULL")
    long countByMacThroughputNotNull();
    
    /**
     * 统计指定速率区间的记录数
     */
    @Query(value = "SELECT COUNT(t) FROM TestData t WHERE t.macThroughput >= :min AND t.macThroughput < :max")
    long countByMacThroughputBetween(@Param("min") Double min, @Param("max") Double max);
    
    /**
     * 统计大于等于指定速率的记录数
     */
    @Query(value = "SELECT COUNT(t) FROM TestData t WHERE t.macThroughput >= :min")
    long countByMacThroughputGreaterThanEqual(@Param("min") Double min);
    
    /**
     * 添加一个用于调试的方法
     */
    @Query("SELECT t.macThroughput FROM TestData t WHERE t.macThroughput IS NOT NULL ORDER BY t.macThroughput")
    List<Double> findAllMacThroughput();

    /**
     * 重置自增ID为1，使新数据从ID=1开始插入
     * 注意: 此操作需要数据库管理员权限
     */
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE test_data AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    
    /**
     * 查询所有数据并按时间排序
     */
    List<TestData> findAllByOrderByTestTimeAsc();
} 