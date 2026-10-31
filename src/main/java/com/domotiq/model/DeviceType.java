package com.domotiq.model;

/** Device categories supported by DOMOTIQ. */
public enum DeviceType {
    LIGHT("Smart Light"),
    FAN("Smart Fan"),
    AC("Smart Air Conditioner"),
    DOOR_LOCK("Smart Door Lock"),
    CAMERA("Smart Camera"),
    SENSOR("Smart Sensor");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
