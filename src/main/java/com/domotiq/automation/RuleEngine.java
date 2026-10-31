package com.domotiq.automation;

import com.domotiq.controller.AutomationController;
import com.domotiq.controller.DeviceController;
import com.domotiq.model.Automation;
import com.domotiq.model.AutomationAction;
import com.domotiq.model.Device;
import com.domotiq.model.devices.SmartSensor;
import com.domotiq.repository.ActivityLogRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Evaluates the user's automations against live events. Three event kinds:
 * sensor readings from the simulator, once-a-minute scheduler ticks, and
 * manual scene runs. Actions are executed through DeviceController, so every
 * automated change is persisted and appears in the activity log with
 * actor = AUTOMATION. All methods must be called on the Swing EDT.
 */
public class RuleEngine {

    /** A schedule fires on the first tick at or after its time, but never
        more than this many minutes late (so restarting the app hours later
        does not replay stale schedules). */
    private static final int SCHEDULE_GRACE_MINUTES = 10;

    private final DeviceController deviceController;
    private final AutomationController automationController;

    private final List<Automation> rules = new ArrayList<>();
    private int userId;

    public RuleEngine(DeviceController deviceController,
                      AutomationController automationController) {
        this.deviceController = deviceController;
        this.automationController = automationController;
    }

    public void loadForUser(int userId) throws SQLException {
        this.userId = userId;
        rules.clear();
        rules.addAll(automationController.loadForUser(userId));
    }

    public List<Automation> getRules() {
        return List.copyOf(rules);
    }

    public void replaceRules(List<Automation> reloaded) {
        rules.clear();
        rules.addAll(reloaded);
    }

    /* ---------- event entry points ---------- */

    public void onSensorReading(SmartSensor sensor, String reading) {
        LocalDateTime now = LocalDateTime.now();
        for (Automation a : rules) {
            if (!a.isEnabled() || a.getTriggerType() != com.domotiq.model.TriggerType.SENSOR) {
                continue;
            }
            if (a.getSensorDeviceId() == null || a.getSensorDeviceId() != sensor.getId()) {
                continue;
            }
            if (!sensorConditionMet(a, reading)) {
                continue;
            }
            if (a.onCooldown(now)) {
                continue;
            }
            fire(a, now);
        }
    }

    /** Called once per minute by the scheduler tick in MainFrame. */
    public void onMinuteTick(LocalDateTime now) {
        for (Automation a : rules) {
            if (!a.isEnabled() || a.getTriggerType() != com.domotiq.model.TriggerType.SCHEDULE) {
                continue;
            }
            if (!scheduleDue(a, now)) {
                continue;
            }
            fire(a, now);
        }
    }

    /** Manual run from the Automations panel. Ignores cooldown by design. */
    public List<String> runScene(int automationId) {
        Automation a = rules.stream()
                .filter(r -> r.getId() == automationId)
                .findFirst().orElse(null);
        if (a == null) {
            return List.of("Scene was deleted while open.");
        }
        return fire(a, LocalDateTime.now());
    }

    /* ---------- evaluation ---------- */

    private boolean scheduleDue(Automation a, LocalDateTime now) {
        if (a.getFireTime() == null) {
            return false;
        }
        LocalDateTime due = LocalDate.from(now).atTime(a.getFireTime());
        if (now.isBefore(due)) {
            return false;
        }
        if (now.isAfter(due.plusMinutes(SCHEDULE_GRACE_MINUTES))) {
            return false;
        }
        if (a.getLastFiredAt() != null && !a.getLastFiredAt().isBefore(due)) {
            return false;
        }
        return a.firesOnDay(now.toLocalDate())
                && a.withinActiveWindow(now.toLocalTime());
    }

    private boolean sensorConditionMet(Automation a, String reading) {
        return switch (a.getSensorOp()) {
            case DETECTED -> {
                String up = reading == null ? "" : reading.toUpperCase();
                yield up.contains("DETECTED") || up.contains("ALERT");
            }
            case GT -> numeric(reading) != null && a.getSensorValue() != null
                    && numeric(reading) > a.getSensorValue();
            case LT -> numeric(reading) != null && a.getSensorValue() != null
                    && numeric(reading) < a.getSensorValue();
            case null -> false;
        };
    }

    /** Simulated readings look like "23.4°C" or "55% RH" - leading number wins. */
    private static Double numeric(String reading) {
        if (reading == null) {
            return null;
        }
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)").matcher(reading);
        return m.find() ? Double.parseDouble(m.group(1)) : null;
    }

    /* ---------- execution ---------- */

    private List<String> fire(Automation a, LocalDateTime now) {
        List<String> problems = new ArrayList<>();
        for (AutomationAction action : a.getActions()) {
            Device device = deviceController.getDevices().stream()
                    .filter(d -> d.getId() == action.getDeviceId())
                    .findFirst().orElse(null);
            if (device == null) {
                problems.add("Action skipped: device no longer exists.");
                continue;
            }
            try {
                if (action.getKind() == AutomationAction.Kind.POWER) {
                    deviceController.setPower(device, action.getPowerTarget(),
                            ActivityLogRepository.ACTOR_AUTOMATION);
                } else {
                    deviceController.applySetting(device, action.getSettingKey(),
                            action.getSettingValue(), ActivityLogRepository.ACTOR_AUTOMATION);
                }
            } catch (IllegalStateException offline) {
                problems.add(offline.getMessage());
            } catch (SQLException sql) {
                problems.add("Database error while running " + a.getName() + ": "
                        + sql.getMessage());
            }
        }
        a.setLastFiredAt(now);
        try {
            automationController.getRepository().touchLastFired(a.getId(), now);
        } catch (SQLException ignored) {
            // last_fired_at is bookkeeping; failing it must not undo executed actions
        }
        return problems;
    }
}
