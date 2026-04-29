package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "event_item")
public class EventItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Integer priority;

    @Column(name = "start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;

    @Column(name = "end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;

    private String type;
    private Boolean completed = false;

    // 新增：关联用户ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    public EventItem() {}

    // Getters and Setters
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