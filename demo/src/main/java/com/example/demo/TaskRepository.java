package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@Repository
public class TaskRepository {
    @Autowired
    private JdbcTemplate jdbc;

    private String getTableName(String username) {
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("用户名包含非法字符");
        }
        return "tasks_" + username; // 去掉反引号
    }

    public void createTable(String username) {
        String sql = "CREATE TABLE IF NOT EXISTS " + getTableName(username) + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(255), description VARCHAR(500), priority INT, " +
                "start_date DATE, end_date DATE, type VARCHAR(50), completed BOOLEAN DEFAULT FALSE)";
        jdbc.execute(sql);
    }

public Long insertTask(String username, EventItem task) {
    // SQL标准语法，INSERT的同时直接返回自增ID
    String sql = "SELECT id FROM FINAL TABLE (INSERT INTO " + getTableName(username) +
            " (title, description, priority, start_date, end_date, type, completed) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?))";
    
    // 一次查询直接拿到ID，不需要额外的函数调用
    return jdbc.queryForObject(sql, Long.class,
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getStart(),
            task.getEnd(),
            task.getType(),
            task.getCompleted() != null ? task.getCompleted() : false
    );
}

    public List<EventItem> findAllTasks(String username) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + getTableName(username));
        return rows.stream().map(row -> {
            EventItem e = new EventItem();
            e.setId(((Number) row.get("id")).longValue());
            e.setTitle((String) row.get("title"));
            e.setDescription((String) row.get("description"));
            e.setPriority((Integer) row.get("priority"));
            e.setStart(row.get("start_date") != null ? ((java.sql.Date) row.get("start_date")).toLocalDate() : null);
            e.setEnd(row.get("end_date") != null ? ((java.sql.Date) row.get("end_date")).toLocalDate() : null);
            e.setType((String) row.get("type"));
            e.setCompleted((Boolean) row.get("completed"));
            return e;
        }).toList();
    }

    public void updateTask(String username, Long id, EventItem task) {
        String sql = "UPDATE " + getTableName(username) +
                " SET title=?, description=?, priority=?, start_date=?, end_date=?, type=?, completed=? WHERE id=?";
        jdbc.update(sql, task.getTitle(), task.getDescription(), task.getPriority(),
                task.getStart(), task.getEnd(), task.getType(), task.getCompleted(), id);
    }

    public void deleteTask(String username, Long id) {
        jdbc.update("DELETE FROM " + getTableName(username) + " WHERE id=?", id);
    }
}