package com.domotiq.model.devices;

import com.domotiq.model.Device;
import static com.domotiq.model.Device.SettingDefinition.combo;
import static com.domotiq.model.Device.SettingDefinition.slider;
import static com.domotiq.model.Device.SettingDefinition.toggle;
import com.domotiq.model.DeviceType;

import java.util.List;

public class SmartAirConditioner extends Device {

    public static final String TARGET_TEMP = "target_temp";
    public static final String MODE = "mode";

    public SmartAirConditioner() {
        super(DeviceType.AC);
    }

    @Override
    public int nominalWatts() { return 1200; }

    @Override
    public String onLabel() { return "ON"; }

    @Override
    public String offLabel() { return "OFF"; }

    @Override
    public String statusLine() {
        if (!online) return "No signal";
        if (!on) return "OFF";
        return "ON · " + settings.getOrDefault(MODE, "") + " · "
                + settings.getOrDefault(TARGET_TEMP, "0") + "°C";
    }

    @Override
    public List<SettingDefinition> settingDefinitions() {
        return List.of(
                slider(TARGET_TEMP, "Target temperature (°C)", 16, 30),
                combo(MODE, "Operating mode", "COOL", "HEAT", "FAN", "DRY", "AUTO"));
    }

    @Override
    protected void applyCategoryDefaults() {
        settings.put(TARGET_TEMP, "22");
        settings.put(MODE, "COOL");
    }
}
