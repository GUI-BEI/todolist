package com.example.demo;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

// 移除JPA实体注解，仅保留数据结构
public class EventItem {
    private Long id;
    private String title;
    private String description;
    private Integer priority;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;

    private String type;
    private Boolean completed = false;

    // 关联用户ID（仍保留，用于逻辑关联）
    private Long userId;

    public EventItem() {}

    // 所有Getters and Setters 保持不变
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public LocalDate getStart() { return start; }
    public void setStart(LocalDate start) { this.start = start; }

    public LocalDate getEnd() { return end; }
    public void setEnd(LocalDate end) { this.end = end; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}