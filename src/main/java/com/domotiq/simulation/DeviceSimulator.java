package com.domotiq.simulation;

import com.domotiq.model.Device;
import com.domotiq.model.devices.SmartSensor;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Produces believable live data for simulated devices. Runs on the Swing
 * event dispatch thread via javax.swing.Timer, so callers can update the
 * GUI directly from the onChange callback. No real IoT hardware is
 * involved - readings are randomly generated telemetry.
 */
public class DeviceSimulator {

    private static final int TICK_MS = 4_000;

    private final Timer timer;
    private final Random random = new Random();
    private final Supplier<List<Device>> deviceSource;
    private Consumer<Device> onChange = d -> { };

    /** Temperature drift model: one baseline per sensor id. */
    private double temperatureBase = 22.0;
    private double humidityBase = 48.0;

    public DeviceSimulator(Supplier<List<Device>> deviceSource) {
        this.timer = new Timer(TICK_MS, this::tick);
        this.timer.setInitialDelay(TICK_MS);
        this.deviceSource = deviceSource;
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void setOnChange(Consumer<Device> onChange) {
        this.onChange = onChange == null ? d -> { } : onChange;
    }

    private void tick(ActionEvent e) {
        for (Device device : deviceSource.get()) {
            if (!device.isOnline() || !(device instanceof SmartSensor sensor) || !sensor.isOn()) {
                continue;
            }
            String kind = sensor.getSettings().getOrDefault(
                    SmartSensor.SENSOR_KIND, "TEMPERATURE");
            sensor.setLastReading(nextReading(kind));
            onChange.accept(sensor);
        }
    }

    private String nextReading(String kind) {
        return switch (kind) {
            case "TEMPERATURE" -> {
                temperatureBase += (random.nextDouble() - 0.5) * 0.6;
                temperatureBase = Math.clamp(temperatureBase, 15.0, 35.0);
                yield String.format("%.1f\u00B0C", temperatureBase);
            }
            case "HUMIDITY" -> {
                humidityBase += (random.nextDouble() - 0.5) * 1.8;
                humidityBase = Math.clamp(humidityBase, 20.0, 80.0);
                yield String.format("%.0f%% RH", humidityBase);
            }
            case "MOTION" -> random.nextInt(4) == 0 ? "MOTION DETECTED" : "no motion";
            case "SMOKE" -> random.nextInt(30) == 0 ? "ALERT: smoke detected" : "clear";
            default -> "n/a";
        };
    }
}
