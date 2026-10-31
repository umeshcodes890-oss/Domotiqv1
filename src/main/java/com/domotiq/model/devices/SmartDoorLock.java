package com.domotiq.model.devices;

import com.domotiq.model.Device;
import static com.domotiq.model.Device.SettingDefinition.combo;
import static com.domotiq.model.Device.SettingDefinition.slider;
import static com.domotiq.model.Device.SettingDefinition.toggle;
import com.domotiq.model.DeviceType;

import java.util.List;

/** For a door lock the power state means LOCKED (on) / UNLOCKED (off). */
public class SmartDoorLock extends Device {

    public static final String AUTO_LOCK_MINUTES = "auto_lock_minutes";

    public SmartDoorLock() {
        super(DeviceType.DOOR_LOCK);
    }

    @Override
    public int nominalWatts() { return 3; }

    @Override
    public String onLabel() { return "LOCKED"; }

    @Override
    public String offLabel() { return "UNLOCKED"; }

    @Override
    public boolean isOn() {
        return on;
    }

    @Override
    public String statusLine() {
        if (!online) return "No signal";
        return on ? "LOCKED" : "UNLOCKED";
    }

    @Override
    public List<SettingDefinition> settingDefinitions() {
        return List.of(slider(AUTO_LOCK_MINUTES, "Auto-lock after unlock (min, 0=off)", 0, 60));
    }

    @Override
    protected void applyCategoryDefaults() {
        settings.put(AUTO_LOCK_MINUTES, "5");
        on = true; // a new lock ships locked
    }
}
