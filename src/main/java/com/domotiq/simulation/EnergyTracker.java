package com.domotiq.simulation;

import com.domotiq.model.Device;
import com.domotiq.repository.EnergyRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Accumulates estimated kWh from simulated uptime: watts x seconds the app
 * observed the device ON. Nothing is measured from hardware; while the app is
 * closed no consumption accrues. Deltas are flushed to MySQL once a minute.
 */
public class EnergyTracker {

    private final Map<Integer, Double> pendingKwh = new HashMap<>();

    /** Called by the simulator for every online + powered device each tick. */
    public void accumulate(Device device, int seconds) {
        double kwh = device.nominalWatts() * seconds / 3_600_000.0;
        pendingKwh.merge(device.getId(), kwh, Double::sum);
    }

    public void flush(EnergyRepository repository, int userId) throws SQLException {
        if (pendingKwh.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        Map<Integer, Double> snapshot = new HashMap<>(pendingKwh);
        pendingKwh.clear();
        for (Map.Entry<Integer, Double> e : snapshot.entrySet()) {
            if (e.getValue() > 0) {
                repository.addKwh(e.getKey(), userId, today, e.getValue());
            }
        }
    }
}
