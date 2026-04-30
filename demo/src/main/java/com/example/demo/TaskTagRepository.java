package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskTagRepository extends JpaRepository<TaskTag, Long> {

    List<TaskTag> findByUserIdOrderBySortOrderAsc(Long userId);

    boolean existsByUserIdAndTagName(Long userId, String tagName);

    void deleteByUserIdAndTagName(Long userId, String tagName);
}