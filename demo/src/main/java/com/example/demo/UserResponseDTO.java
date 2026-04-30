package com.example.demo;

import java.time.LocalDateTime;

public class UserResponseDTO {
    private Long id;
    private String username;
    private Integer totalDays;
    private LocalDateTime lastSignDate;
    private String token;

    // 构造函数1：普通用户信息（无token）
    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.totalDays = user.getTotalDays();
        this.lastSignDate = user.getLastSignDate();
        this.token = null;
    }

    // 构造函数2：登录专用（带token）
    public UserResponseDTO(User user, String token) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.totalDays = user.getTotalDays();
        this.lastSignDate = user.getLastSignDate();
        this.token = token;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public Integer getTotalDays() { return totalDays; }
    public LocalDateTime getLastSignDate() { return lastSignDate; }
    public String getToken() { return token; }
}