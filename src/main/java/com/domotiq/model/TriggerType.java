package com.domotiq.model;

/** How an automation is started. SCENE rules only run when the user presses Run. */
public enum TriggerType {
    SCENE("Scene (manual run)"),
    SCHEDULE("Time schedule"),
    SENSOR("Sensor threshold");

    private final String label;

    TriggerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
