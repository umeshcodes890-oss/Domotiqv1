package com.domotiq.factory;

import com.domotiq.model.Device;
import com.domotiq.model.DeviceType;
import com.domotiq.model.devices.SmartAirConditioner;
import com.domotiq.model.devices.SmartCamera;
import com.domotiq.model.devices.SmartDoorLock;
import com.domotiq.model.devices.SmartFan;
import com.domotiq.model.devices.SmartLight;
import com.domotiq.model.devices.SmartSensor;

/**
 * Single place that knows how every DeviceType maps to a concrete class.
 * New categories are added here plus a model class — nothing else changes.
 */
public final class DeviceFactory {

    private DeviceFactory() {
    }

    public static Device create(DeviceType type, String name, int userId, Integer roomId) {
        Device device = create(type);
        device.setName(name);
        device.setUserId(userId);
        device.setRoomId(roomId);
        return device;
    }

    public static Device create(DeviceType type) {
        Device device = switch (type) {
            case LIGHT -> new SmartLight();
            case FAN -> new SmartFan();
            case AC -> new SmartAirConditioner();
            case DOOR_LOCK -> new SmartDoorLock();
            case CAMERA -> new SmartCamera();
            case SENSOR -> new SmartSensor();
        };
        device.applyDefaults();
        return device;
    }
}
