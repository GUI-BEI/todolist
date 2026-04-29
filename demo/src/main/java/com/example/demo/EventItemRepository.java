package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class EventItemRepository {

    private final JdbcTemplate jdbcTemplate;

    // 注入JdbcTemplate
    public EventItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 【删除】之前临时加的 count() 方法，保持代码整洁

    // 自定义RowMapper，解决日期字段映射问题
    private final RowMapper<EventItem> eventItemRowMapper = (rs, rowNum) -> {
        EventItem item = new EventItem();
        item.setId(rs.getLong("id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setPriority(rs.getObject("priority", Integer.class));
        item.setStart(rs.getObject("start_date", LocalDate.class));
        item.setEnd(rs.getObject("end_date", LocalDate.class));
        item.setType(rs.getString("type"));
        item.setCompleted(rs.getBoolean("completed"));
        item.setUserId(rs.getLong("user_id"));
        return item;
    };

    // 为指定用户创建任务表（适配H2数据库）
    public void createTaskTableForUser(Long userId) {
        String tableName = getTaskTableName(userId);
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                priority INT,
                start_date DATE,
                end_date DATE,
                type VARCHAR(50),
                completed BOOLEAN DEFAULT FALSE,
                user_id BIGINT NOT NULL
            )
            """, tableName);
        jdbcTemplate.execute(createTableSql);
    }

    // 动态生成用户任务表名
    private String getTaskTableName(Long userId) {
        return "user_" + userId + "_tasks";
    }

    // 新增任务
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

    // 根据用户ID查询所有任务
    public List<EventItem> findByUserId(Long userId) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("SELECT * FROM %s WHERE user_id = ?", tableName);
        return jdbcTemplate.query(sql, eventItemRowMapper, userId);
    }

    // 根据ID查询任务（需指定用户ID）
    public EventItem findById(Long userId, Long taskId) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("SELECT * FROM %s WHERE id = ? AND user_id = ?", tableName);
        List<EventItem> result = jdbcTemplate.query(sql, eventItemRowMapper, taskId, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    // 更新任务
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

    // 删除任务
    public void deleteById(Long userId, Long taskId) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("DELETE FROM %s WHERE id = ? AND user_id = ?", tableName);
        jdbcTemplate.update(sql, taskId, userId);
    }

    // 仅更新任务时间
    public void updateTaskTime(Long userId, Long taskId, LocalDate start, LocalDate end) {
        String tableName = getTaskTableName(userId);
        String sql = String.format("""
            UPDATE %s SET start_date = ?, end_date = ? 
            WHERE id = ? AND user_id = ?
            """, tableName);
        jdbcTemplate.update(sql, start, end, taskId, userId);
    }

    // 筛选任务（使用参数绑定，彻底避免SQL注入）
    public List<EventItem> findByFilter(Long userId, Integer priority, String keyword, LocalDate startDate, LocalDate endDate) {
        String tableName = getTaskTableName(userId);
        StringBuilder sql = new StringBuilder(String.format("SELECT * FROM %s WHERE user_id = ?", tableName));
        List<Object> params = new ArrayList<>();
        params.add(userId);

        // 优先级筛选（参数绑定）
        if (priority != null) {
            sql.append(" AND priority = ?");
            params.add(priority);
        }

        // 关键词筛选（参数绑定，避免LIKE注入）
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)");
            params.add("%" + kw + "%");
            params.add("%" + kw + "%");
        }

        // 日期范围筛选（参数绑定）
        if (startDate != null && endDate != null) {
            sql.append(" AND NOT (end_date < ? OR start_date > ?)");
            params.add(startDate);
            params.add(endDate);
        }

        return jdbcTemplate.query(sql.toString(), eventItemRowMapper, params.toArray());
    }
}