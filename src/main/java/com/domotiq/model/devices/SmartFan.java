package com.domotiq.model.devices;

import com.domotiq.model.Device;
import static com.domotiq.model.Device.SettingDefinition.combo;
import static com.domotiq.model.Device.SettingDefinition.slider;
import static com.domotiq.model.Device.SettingDefinition.toggle;
import com.domotiq.model.DeviceType;

import java.util.List;

public class SmartFan extends Device {

    public static final String SPEED = "speed";
    public static final String OSCILLATE = "oscillate";

    public SmartFan() {
        super(DeviceType.FAN);
    }

    @Override
    public String onLabel() { return "ON"; }

    @Override
    public String offLabel() { return "OFF"; }

    @Override
    public String statusLine() {
        if (!online) return "No signal";
        if (!on) return "OFF";
        boolean osc = Boolean.parseBoolean(settings.getOrDefault(OSCILLATE, "false"));
        return "ON · speed " + settings.getOrDefault(SPEED, "0")
                + (osc ? " · oscillating" : "");
    }

    @Override
    public List<SettingDefinition> settingDefinitions() {
        return List.of(
                slider(SPEED, "Speed", 1, 3),
                toggle(OSCILLATE, "Oscillation"));
    }

    @Override
    protected void applyCategoryDefaults() {
        settings.put(SPEED, "2");
        settings.put(OSCILLATE, "false");
    }
}
