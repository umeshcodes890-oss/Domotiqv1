package com.domotiq.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Common contract for every DOMOTIQ device. Concrete categories declare
 * their own settings through {@link #settingDefinitions()} so the GUI and
 * the repository stay generic — adding a new category never touches them.
 *
 * Devices are SIMULATED: state lives in memory and MySQL, no real hardware.
 */
public abstract class Device {

    public enum ControlKind { SLIDER, COMBO, TOGGLE }

    /** Describes one configurable setting so generic UI code can render it. */
    public static final class SettingDefinition {
        public final String key;
        public final String label;
        public final ControlKind kind;
        public final int min;
        public final int max;
        public final List<String> options;

        private SettingDefinition(String key, String label, ControlKind kind,
                                  int min, int max, List<String> options) {
            this.key = key;
            this.label = label;
            this.kind = kind;
            this.min = min;
            this.max = max;
            this.options = options;
        }

        public static SettingDefinition slider(String key, String label, int min, int max) {
            return new SettingDefinition(key, label, ControlKind.SLIDER, min, max, List.of());
        }

        public static SettingDefinition combo(String key, String label, String... options) {
            return new SettingDefinition(key, label, ControlKind.COMBO, 0, 0, List.of(options));
        }

        public static SettingDefinition toggle(String key, String label) {
            return new SettingDefinition(key, label, ControlKind.TOGGLE, 0, 0, List.of());
        }
    }

    protected int id = -1;
    protected int userId;
    protected Integer roomId;
    protected String name;
    protected final DeviceType deviceType;
    protected boolean on;
    protected boolean online = true;
    protected LocalDateTime dateAdded;
    protected final Map<String, String> settings = new LinkedHashMap<>();

    protected Device(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    /* ---------- identity / persistence ---------- */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public DeviceType getDeviceType() { return deviceType; }
    public boolean isOn() { return on; }
    /** Restores persisted power state without behaviour side effects. Repository use only. */
    public final void setOnInternal(boolean on) {
        this.on = on;
    }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public LocalDateTime getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDateTime dateAdded) { this.dateAdded = dateAdded; }

    public Map<String, String> getSettings() {
        return Collections.unmodifiableMap(settings);
    }

    /** Loads persisted settings without validation (called by the repository). */
    public void loadSettings(Map<String, String> persisted) {
        settings.clear();
        settings.putAll(persisted);
    }

    /* ---------- behaviour ---------- */

    /** Label used for the ON state, e.g. "ON" or "LOCKED". */
    public abstract String onLabel();

    /** Label used for the OFF state, e.g. "OFF" or "UNLOCKED". */
    public abstract String offLabel();

    /** One-line human readable operating summary, used in cards and logs. */
    public abstract String statusLine();

    /**
     * Nameplate draw used for the ENERGY ESTIMATES in the simulator. These are
     * typical figures for the category, not measurements from real hardware.
     */
    public abstract int nominalWatts();

    /** Settings this category exposes to the GUI and the database. */
    public abstract List<SettingDefinition> settingDefinitions();

    /** Turn the device on/off. Returns false when the device is offline. */
    public final boolean setPower(boolean requested) {
        if (!online) {
            return false;
        }
        on = requested;
        onPowerChanged();
        return true;
    }

    protected void onPowerChanged() {
        // subclasses may adjust dependent state
    }

    public final void setSetting(String key, String value) {
        validateSettingValue(key, value);
        settings.put(key, value);
        onSettingChanged(key, value);
    }

    protected void validateSettingValue(String key, String value) {
        SettingDefinition def = settingDefinitions().stream()
                .filter(d -> d.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        name + " has no setting '" + key + "'"));
        switch (def.kind) {
            case SLIDER -> {
                int v = Integer.parseInt(value);
                if (v < def.min || v > def.max) {
                    throw new IllegalArgumentException(
                            def.label + " must be between " + def.min + " and " + def.max);
                }
            }
            case COMBO -> {
                if (!def.options.contains(value)) {
                    throw new IllegalArgumentException("Invalid " + def.label + ": " + value);
                }
            }
            case TOGGLE -> {
                if (!value.equals("true") && !value.equals("false")) {
                    throw new IllegalArgumentException("Toggle value must be true/false");
                }
            }
        }
    }

    protected void onSettingChanged(String key, String value) {
        // hook for category-specific side effects
    }

    /** Restore default settings for the category (used when a device is created). */
    public void applyDefaults() {
        for (SettingDefinition def : settingDefinitions()) {
            switch (def.kind) {
                case SLIDER -> settings.putIfAbsent(def.key, String.valueOf(def.min));
                case COMBO -> settings.putIfAbsent(def.key, def.options.get(0));
                case TOGGLE -> settings.putIfAbsent(def.key, "false");
            }
        }
        applyCategoryDefaults();
    }

    protected void applyCategoryDefaults() {
    }
}
