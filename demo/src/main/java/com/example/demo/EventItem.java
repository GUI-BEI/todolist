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

    // 核心修改：给 start 和 end 换个非关键字的列名
    @Column(name = "start_date") // 映射到数据库的 start_date 列
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;

    @Column(name = "end_date")   // 映射到数据库的 end_date 列
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;

    private String type;
    private Boolean completed = false;

    public EventItem() {}

    // ... 其他 getter/setter 保持不变 ...
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
}