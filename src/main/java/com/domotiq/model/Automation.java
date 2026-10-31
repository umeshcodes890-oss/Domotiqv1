package com.domotiq.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A user-defined rule: a scene, a time schedule, or a sensor threshold.
 * Holds its ordered actions and the bookkeeping the rule engine needs
 * (enabled flag, cooldown, last fired time).
 */
public class Automation {

    /** Comparison used by SENSOR triggers. */
    public enum SensorOp {
        GT("is above"),
        LT("is below"),
        DETECTED("reports detection");

        private final String label;

        SensorOp(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private int id = -1;
    private int userId;
    private String name;
    private TriggerType triggerType;
    private boolean enabled = true;

    // SENSOR trigger fields
    private Integer sensorDeviceId;
    private SensorOp sensorOp;
    private Double sensorValue;

    // SCHEDULE trigger fields
    private LocalTime fireTime;
    private String daysMask = "1111111";           // Mon..Sun as 1/0
    private LocalTime activeStart;
    private LocalTime activeEnd;

    // shared
    private int cooldownMinutes = 10;
    private LocalDateTime lastFiredAt;

    private final List<AutomationAction> actions = new ArrayList<>();

    /* ---------- evaluation helpers ---------- */

    public boolean firesOnDay(LocalDate date) {
        if (daysMask == null || daysMask.length() != 7) {
            return true;
        }
        return daysMask.charAt(date.getDayOfWeek().getValue() - 1) == '1';
    }

    public boolean withinActiveWindow(LocalTime now) {
        if (activeStart == null || activeEnd == null) {
            return true;
        }
        return !now.isBefore(activeStart) && !now.isAfter(activeEnd);
    }

    public boolean onCooldown(LocalDateTime now) {
        return lastFiredAt != null
                && now.isBefore(lastFiredAt.plusMinutes(Math.max(0, cooldownMinutes)));
    }

    /* ---------- accessors ---------- */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Integer getSensorDeviceId() { return sensorDeviceId; }
    public void setSensorDeviceId(Integer sensorDeviceId) { this.sensorDeviceId = sensorDeviceId; }
    public SensorOp getSensorOp() { return sensorOp; }
    public void setSensorOp(SensorOp sensorOp) { this.sensorOp = sensorOp; }
    public Double getSensorValue() { return sensorValue; }
    public void setSensorValue(Double sensorValue) { this.sensorValue = sensorValue; }
    public LocalTime getFireTime() { return fireTime; }
    public void setFireTime(LocalTime fireTime) { this.fireTime = fireTime; }
    public String getDaysMask() { return daysMask; }
    public void setDaysMask(String daysMask) { this.daysMask = daysMask; }
    public LocalTime getActiveStart() { return activeStart; }
    public void setActiveStart(LocalTime activeStart) { this.activeStart = activeStart; }
    public LocalTime getActiveEnd() { return activeEnd; }
    public void setActiveEnd(LocalTime activeEnd) { this.activeEnd = activeEnd; }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public LocalDateTime getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(LocalDateTime lastFiredAt) { this.lastFiredAt = lastFiredAt; }
    public List<AutomationAction> getActions() { return actions; }
}
