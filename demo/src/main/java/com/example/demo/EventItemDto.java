package com.example.demo;

import java.time.LocalDate;

public class EventItemDto {
    private Long id;
    private String title;
    private String description;
    private Integer priority;   // 或者 int
    private LocalDate start;
    private LocalDate end;
    private String type;  // 任务类型：工作、学习、娱乐等
    private Boolean completed = false;  // 默认为未完成
    public void setId(Long id) {this.id = id;}
    public void setTitle(String title) {this.title = title;}
    public void setDescription(String description) {this.description = description;}
    public void setPriority(Integer priority) {this.priority = priority;}
    public void setStart(LocalDate start) {this.start = start;}
    public void setEnd(LocalDate end) {this.end = end;}
    public void setType(String type) {this.type = type;}
    public Long getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public Integer getPriority() {
        return priority;
    }
    public LocalDate getStart() {
        return start;
    }
    public LocalDate getEnd() {
        return end;
    }
    public String getType() {
        return type;
    }
    public Boolean getCompleted() {
        return completed;
    }
}
