package com.example.demo;

public class SignStatusDTO {
    private Integer totalDays;
    private Boolean signedToday;

    public SignStatusDTO(Integer totalDays, Boolean signedToday) {
        this.totalDays = totalDays;
        this.signedToday = signedToday;
    }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }

    public Boolean getSignedToday() { return signedToday; }
    public void setSignedToday(Boolean signedToday) { this.signedToday = signedToday; }
}