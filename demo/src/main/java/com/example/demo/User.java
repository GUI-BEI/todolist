package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user") // 避免SQL保留字
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(name = "total_days")
    private Integer totalDays = 0;

    @Column(name = "last_sign_date")
    private LocalDateTime lastSignDate;

    @Column(name = "token")
    private String token;

    public User() {}
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.totalDays = 0;
        this.lastSignDate = null;
    }

    // 所有Getters and Setters保持不变
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }
    public LocalDateTime getLastSignDate() { return lastSignDate; }
    public void setLastSignDate(LocalDateTime lastSignDate) { this.lastSignDate = lastSignDate; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}