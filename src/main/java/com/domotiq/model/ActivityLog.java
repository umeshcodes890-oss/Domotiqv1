package com.domotiq.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private long id;
    private int userId;
    private Integer deviceId;
    private String deviceName;
    private String action;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime performedAt;

    public ActivityLog() {
    }

    public ActivityLog(long id, int userId, Integer deviceId, String deviceName,
                       String action, String previousStatus, String newStatus,
                       LocalDateTime performedAt) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.performedAt = performedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}
