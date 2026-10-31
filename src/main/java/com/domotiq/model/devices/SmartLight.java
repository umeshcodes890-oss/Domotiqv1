package com.domotiq.model.devices;

import com.domotiq.model.Device;
import static com.domotiq.model.Device.SettingDefinition.combo;
import static com.domotiq.model.Device.SettingDefinition.slider;
import static com.domotiq.model.Device.SettingDefinition.toggle;
import com.domotiq.model.DeviceType;

import java.util.List;

public class SmartLight extends Device {

    public static final String BRIGHTNESS = "brightness";
    public static final String COLOR_TEMP = "color_temp";

    public SmartLight() {
        super(DeviceType.LIGHT);
    }

    @Override
    public int nominalWatts() { return 15; }

    @Override
    public String onLabel() { return "ON"; }

    @Override
    public String offLabel() { return "OFF"; }

    @Override
    public String statusLine() {
        if (!online) return "No signal";
        if (!on) return "OFF";
        return "ON · " + settings.getOrDefault(BRIGHTNESS, "0") + "% · "
                + settings.getOrDefault(COLOR_TEMP, "");
    }

    @Override
    public List<SettingDefinition> settingDefinitions() {
        return List.of(
                slider(BRIGHTNESS, "Brightness (%)", 1, 100),
                combo(COLOR_TEMP, "Colour temperature", "WARM", "NEUTRAL", "COOL"));
    }

    @Override
    protected void applyCategoryDefaults() {
        settings.put(BRIGHTNESS, "80");
        settings.put(COLOR_TEMP, "WARM");
    }
}
