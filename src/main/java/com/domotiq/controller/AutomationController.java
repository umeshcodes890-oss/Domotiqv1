package com.domotiq.controller;

import com.domotiq.model.Automation;
import com.domotiq.model.AutomationAction;
import com.domotiq.model.Device;
import com.domotiq.model.TriggerType;
import com.domotiq.repository.AutomationRepository;

import java.sql.SQLException;
import java.util.List;

/** Validation and persistence gateway for automations/scenes. GUI use only. */
public class AutomationController {

    private final AutomationRepository repository = new AutomationRepository();

    public List<Automation> loadForUser(int userId) throws SQLException {
        return repository.findByUser(userId);
    }

    public int countFiredToday(int userId) throws SQLException {
        return repository.countFiredToday(userId);
    }

    public void save(int userId, Automation automation) throws SQLException {
        validate(userId, automation);
        automation.setUserId(userId);
        if (automation.getId() == -1) {
            repository.insert(automation);
        } else {
            repository.update(automation);
        }
    }

    public void delete(int userId, Automation automation) throws SQLException {
        repository.delete(automation.getId(), userId);
    }

    public void setEnabled(int userId, Automation automation, boolean enabled)
            throws SQLException {
        automation.setEnabled(enabled);
        repository.setEnabled(automation.getId(), userId, enabled);
    }

    public AutomationRepository getRepository() {
        return repository;
    }

    /* ---------- validation ---------- */

    private void validate(int userId, Automation a) {
        if (a.getName() == null || a.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Rule name must be at least 2 characters.");
        }
        if (a.getTriggerType() == null) {
            throw new IllegalArgumentException("Choose a trigger type.");
        }
        if (a.getActions().isEmpty()) {
            throw new IllegalArgumentException(
                    "Add at least one device action (a rule with no steps does nothing).");
        }
        switch (a.getTriggerType()) {
            case SCHEDULE -> {
                if (a.getFireTime() == null) {
                    throw new IllegalArgumentException("Set a daily fire time (HH:mm).");
                }
                if (a.getDaysMask() == null || a.getDaysMask().chars().noneMatch(c -> c == '1')) {
                    throw new IllegalArgumentException("Pick at least one day of the week.");
                }
            }
            case SENSOR -> {
                if (a.getSensorDeviceId() == null) {
                    throw new IllegalArgumentException("Select the sensor to watch.");
                }
                if (a.getSensorOp() == null) {
                    throw new IllegalArgumentException("Choose a sensor condition.");
                }
                if (a.getSensorOp() != Automation.SensorOp.DETECTED
                        && a.getSensorValue() == null) {
                    throw new IllegalArgumentException("Enter the threshold value to compare.");
                }
            }
            default -> { }
        }
        if (a.getCooldownMinutes() < 0 || a.getCooldownMinutes() > 24 * 60) {
            throw new IllegalArgumentException("Cooldown must be between 0 and 1440 minutes.");
        }
    }

    /** Used by the rule builder to check a SET action value against the device model. */
    public static void validateAction(Device device, AutomationAction action) {
        if (action.getKind() == AutomationAction.Kind.SET) {
            device.setSetting(action.getSettingKey(), action.getSettingValue());
        }
    }

    public static String describeTrigger(Automation a, String sensorName) {
        return switch (a.getTriggerType()) {
            case SCENE -> "Manual scene";
            case SCHEDULE -> "At " + a.getFireTime() + " on " + describeDays(a.getDaysMask())
                    + (a.getActiveStart() != null
                            ? " (only between " + a.getActiveStart() + " and "
                            + a.getActiveEnd() + ")" : "");
            case SENSOR -> sensorName + " "
                    + (a.getSensorOp() == Automation.SensorOp.DETECTED
                            ? Automation.SensorOp.DETECTED.getLabel()
                            : a.getSensorOp().getLabel() + " " + trimNumber(a.getSensorValue()));
        };
    }

    private static String trimNumber(Double v) {
        if (v == null) {
            return "?";
        }
        return v == Math.rint(v) ? String.valueOf(v.longValue()) : String.valueOf(v);
    }

    private static String describeDays(String mask) {
        String[] names = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        if (mask == null || mask.length() != 7) {
            return "every day";
        }
        if ("1111111".equals(mask)) {
            return "every day";
        }
        if ("1111100".equals(mask)) {
            return "weekdays";
        }
        if ("0000011".equals(mask)) {
            return "weekends";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (mask.charAt(i) == '1') {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(names[i]);
            }
        }
        return sb.length() == 0 ? "every day" : sb.toString();
    }
}
