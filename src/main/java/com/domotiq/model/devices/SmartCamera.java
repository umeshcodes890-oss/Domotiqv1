package com.domotiq.model.devices;

import com.domotiq.model.Device;
import static com.domotiq.model.Device.SettingDefinition.combo;
import static com.domotiq.model.Device.SettingDefinition.slider;
import static com.domotiq.model.Device.SettingDefinition.toggle;
import com.domotiq.model.DeviceType;

import java.util.List;

public class SmartCamera extends Device {

    public static final String RESOLUTION = "resolution";
    public static final String NIGHT_VISION = "night_vision";
    public static final String RECORDING = "recording";

    public SmartCamera() {
        super(DeviceType.CAMERA);
    }

    @Override
    public int nominalWatts() { return 12; }

    @Override
    public String onLabel() { return "MONITORING"; }

    @Override
    public String offLabel() { return "STANDBY"; }

    @Override
    public String statusLine() {
        if (!online) return "No signal";
        if (!on) return "STANDBY";
        boolean rec = Boolean.parseBoolean(settings.getOrDefault(RECORDING, "false"));
        return "MONITORING · " + settings.getOrDefault(RESOLUTION, "")
                + (rec ? " · recording" : " · live only");
    }

    @Override
    public List<SettingDefinition> settingDefinitions() {
        return List.of(
                combo(RESOLUTION, "Resolution", "480P", "720P", "1080P", "4K"),
                toggle(NIGHT_VISION, "Night vision"),
                toggle(RECORDING, "Record to cloud"));
    }

    /** Recording without a stream is meaningless. */
    @Override
    protected void onPowerChanged() {
        if (!on) {
            settings.put(RECORDING, "false");
        }
    }

    @Override
    protected void applyCategoryDefaults() {
        settings.put(RESOLUTION, "1080P");
        settings.put(NIGHT_VISION, "false");
        settings.put(RECORDING, "false");
    }
}
