package com.domotiq.model.devices;

import com.domotiq.model.Device;
import static com.domotiq.model.Device.SettingDefinition.combo;
import static com.domotiq.model.Device.SettingDefinition.slider;
import static com.domotiq.model.Device.SettingDefinition.toggle;
import com.domotiq.model.DeviceType;

import java.util.List;

/**
 * A simulated sensor. Sensor kind (what it measures) is a persisted setting;
 * the current reading is volatile live data produced by the simulator and is
 * intentionally never written to the database.
 */
public class SmartSensor extends Device {

    public static final String SENSOR_KIND = "sensor_kind";

    private String lastReading = "—";

    public SmartSensor() {
        super(DeviceType.SENSOR);
    }

    @Override
    public String onLabel() { return "ARMED"; }

    @Override
    public String offLabel() { return "SILENT"; }

    @Override
    public String statusLine() {
        if (!online) return "No signal";
        return (on ? "ARMED" : "SILENT") + " · " + lastReading;
    }

    public String getLastReading() {
        return lastReading;
    }

    public void setLastReading(String lastReading) {
        this.lastReading = lastReading;
    }

    @Override
    public List<SettingDefinition> settingDefinitions() {
        return List.of(combo(SENSOR_KIND, "Measures",
                "TEMPERATURE", "HUMIDITY", "MOTION", "SMOKE"));
    }

    @Override
    protected void applyCategoryDefaults() {
        settings.put(SENSOR_KIND, "TEMPERATURE");
        on = true; // sensors report unless explicitly silenced
    }
}
