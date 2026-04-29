package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventItemRepository extends JpaRepository<EventItem, Long> {

    List<EventItem> findByUserId(Long userId);

    List<EventItem> findByUserIdAndPriority(Long userId, Integer priority);

    List<EventItem> findByUserIdAndTitleContaining(Long userId, String keyword);

    List<EventItem> findByUserIdAndDescriptionContaining(Long userId, String keyword);

    // 使用 LocalDateTime
    List<EventItem> findByUserIdAndStartBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM EventItem e WHERE e.userId = :userId AND (e.title LIKE %:keyword% OR e.description LIKE %:keyword%)")
    List<EventItem> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}