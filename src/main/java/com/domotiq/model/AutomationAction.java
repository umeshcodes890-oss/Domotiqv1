package com.domotiq.model;

/**
 * One ordered step inside an automation: either set a device's power to a
 * fixed target, or write one of its named settings. Mirrors a row of
 * automation_actions.
 */
public class AutomationAction {

    public enum Kind { POWER, SET }

    private int deviceId;
    private Kind kind;
    private Boolean powerTarget;      // used by POWER
    private String settingKey;        // used by SET
    private String settingValue;      // used by SET
    private int sortOrder;

    public static AutomationAction power(int deviceId, boolean target, int sortOrder) {
        AutomationAction a = new AutomationAction();
        a.deviceId = deviceId;
        a.kind = Kind.POWER;
        a.powerTarget = target;
        a.sortOrder = sortOrder;
        return a;
    }

    public static AutomationAction set(int deviceId, String key, String value, int sortOrder) {
        AutomationAction a = new AutomationAction();
        a.deviceId = deviceId;
        a.kind = Kind.SET;
        a.settingKey = key;
        a.settingValue = value;
        a.sortOrder = sortOrder;
        return a;
    }

    public int getDeviceId() { return deviceId; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public Boolean getPowerTarget() { return powerTarget; }
    public void setPowerTarget(Boolean powerTarget) { this.powerTarget = powerTarget; }
    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
