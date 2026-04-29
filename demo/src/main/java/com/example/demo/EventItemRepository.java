package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class EventItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 自定义RowMapper，时间类型统一为 LocalDateTime
    private final RowMapper<EventItem> eventItemRowMapper = (rs, rowNum) -> {
        EventItem item = new EventItem();
        item.setId(rs.getLong("id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setPriority(rs.getObject("priority", Integer.class));
        item.setStart(rs.getObject("start_date", LocalDateTime.class));
        item.setEnd(rs.getObject("end_date", LocalDateTime.class));
        item.setType(rs.getString("type"));
        item.setCompleted(rs.getBoolean("completed"));
        item.setUserId(rs.getLong("user_id"));
        return item;
    };

    // 动态建表：为每个用户创建独立任务表
    public void createTaskTableForUser(Long userId) {
        String tableName = getTaskTableName(userId);
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                priority INT,
                start_date TIMESTAMP,
                end_date TIMESTAMP,
                type VARCHAR(50),
                completed BOOLEAN DEFAULT FALSE,
                user_id BIGINT NOT NULL
            )
            """, tableName);
        jdbcTemplate.execute(createTableSql);
    }

    private String getTaskTableName(Long userId) {
        return "user_" + userId + "_tasks";
    }

    public EventItem save(Long userId, EventItem task) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("""
            INSERT INTO %s (title, description, priority, start_date, end_date, type, completed, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, tableName);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setObject(3, task.getPriority(), java.sql.Types.INTEGER);
            ps.setObject(4, task.getStart());
            ps.setObject(5, task.getEnd());
            ps.setString(6, task.getType());
            ps.setBoolean(7, task.getCompleted() == null ? false : task.getCompleted());
            ps.setLong(8, userId);
            return ps;
        }, keyHolder);
        task.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return task;
    }

    public List<EventItem> findByUserId(Long userId) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("SELECT * FROM %s WHERE user_id = ?", tableName);
        return jdbcTemplate.query(sql, eventItemRowMapper, userId);
    }

    public EventItem findById(Long userId, Long taskId) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("SELECT * FROM %s WHERE id = ? AND user_id = ?", tableName);
        List<EventItem> result = jdbcTemplate.query(sql, eventItemRowMapper, taskId, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    public EventItem update(Long userId, EventItem task) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("""
            UPDATE %s SET 
                title = ?, 
                description = ?, 
                priority = ?, 
                start_date = ?, 
                end_date = ?, 
                type = ?, 
                completed = ? 
            WHERE id = ? AND user_id = ?
            """, tableName);
        jdbcTemplate.update(sql,
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStart(),
                task.getEnd(),
                task.getType(),
                task.getCompleted(),
                task.getId(),
                userId);
        return task;
    }

    public void deleteById(Long userId, Long taskId) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("DELETE FROM %s WHERE id = ? AND user_id = ?", tableName);
        jdbcTemplate.update(sql, taskId, userId);
    }

    public void updateTaskTime(Long userId, Long taskId, LocalDateTime newStart, LocalDateTime newEnd) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("""
            UPDATE %s SET start_date = ?, end_date = ? 
            WHERE id = ? AND user_id = ?
            """, tableName);
        jdbcTemplate.update(sql, newStart, newEnd, taskId, userId);
    }

    public List<EventItem> findByFilter(Long userId, Integer priority, String keyword, 
                                       LocalDateTime startDate, LocalDateTime endDate) {
        String tableName = getTaskTableName(userId);
        StringBuilder sql = new StringBuilder(String.format("SELECT * FROM %s WHERE user_id = ?", tableName));
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (priority != null) {
            sql.append(" AND priority = ?");
            params.add(priority);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)");
            params.add("%" + kw + "%");
            params.add("%" + kw + "%");
        }

        if (startDate != null && endDate != null) {
            sql.append(" AND NOT (end_date < ? OR start_date > ?)");
            params.add(startDate);
            params.add(endDate);
        }

        return jdbcTemplate.query(sql.toString(), eventItemRowMapper, params.toArray());
    }
}