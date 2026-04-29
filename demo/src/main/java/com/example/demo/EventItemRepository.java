package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventItemRepository extends JpaRepository<EventItem, Long> {

    // 根据用户ID查询所有任务
    List<EventItem> findByUserId(Long userId);

    // 根据用户ID和优先级查询
    List<EventItem> findByUserIdAndPriority(Long userId, Integer priority);

    // 根据用户ID和标题关键词搜索
    List<EventItem> findByUserIdAndTitleContaining(Long userId, String keyword);

    // 根据用户ID和描述关键词搜索
    List<EventItem> findByUserIdAndDescriptionContaining(Long userId, String keyword);

    // 根据用户ID和日期范围查询（开始日期在范围内）
    List<EventItem> findByUserIdAndStartDateBetween(Long userId, LocalDate start, LocalDate end);

    // 组合查询：标题或描述包含关键词
    @Query("SELECT e FROM EventItem e WHERE e.userId = :userId AND (e.title LIKE %:keyword% OR e.description LIKE %:keyword%)")
    List<EventItem> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}