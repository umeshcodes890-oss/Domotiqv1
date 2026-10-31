package com.domotiq.controller;

import com.domotiq.factory.DeviceFactory;
import com.domotiq.model.Device;
import com.domotiq.model.DeviceType;
import com.domotiq.repository.ActivityLogRepository;
import com.domotiq.repository.DeviceRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates every device operation: validates, mutates the in-memory
 * model, writes through to MySQL, and records the activity log. The GUI
 * never talks to repositories directly.
 */
public class DeviceController {

    private final DeviceRepository deviceRepository = new DeviceRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    private final List<Device> devices = new ArrayList<>();
    private int currentUserId;

    public void loadForUser(int userId) throws SQLException {
        this.currentUserId = userId;
        devices.clear();
        devices.addAll(deviceRepository.findByUser(userId));
    }

    public List<Device> getDevices() {
        return List.copyOf(devices);
    }

    public List<Device> search(String query, DeviceType typeFilter, Integer roomIdFilter) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Device> result = devices.stream()
                .filter(d -> q.isEmpty()
                        || d.getName().toLowerCase().contains(q)
                        || d.getDeviceType().getLabel().toLowerCase().contains(q))
                .filter(d -> typeFilter == null || d.getDeviceType() == typeFilter)
                .filter(d -> roomIdFilter == null || roomIdFilter.equals(d.getRoomId()))
                .sorted(Comparator.comparing(Device::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ArrayList<>(result);
    }

    public Device addDevice(String name, DeviceType type, Integer roomId,
                            Map<String, String> initialSettings) throws SQLException {
        validateName(name);
        Device device = DeviceFactory.create(type, name.trim(), currentUserId, roomId);
        if (initialSettings != null) {
            initialSettings.forEach(device::setSetting);
        }
        device.setDateAdded(LocalDateTime.now());
        deviceRepository.insert(device);
        devices.add(device);
        activityLogRepository.insert(currentUserId, device.getId(), device.getName(),
                "ADDED", null, device.statusLine());
        return device;
    }

    public void updateDevice(Device device, String newName, Integer roomId,
                             Map<String, String> newSettings) throws SQLException {
        validateName(newName);
        Device existing = require(device.getId());

        String before = existing.statusLine();
        existing.setName(newName.trim());
        existing.setRoomId(roomId);
        if (newSettings != null) {
            newSettings.forEach(existing::setSetting);
        }
        deviceRepository.update(existing);
        if (!before.equals(existing.statusLine())) {
            activityLogRepository.insert(currentUserId, existing.getId(),
                    existing.getName(), "UPDATED_SETTINGS", before, existing.statusLine());
        }
    }

    public void deleteDevice(int deviceId) throws SQLException {
        Device device = require(deviceId);
        deviceRepository.delete(deviceId, currentUserId);
        devices.remove(device);
        activityLogRepository.insert(currentUserId, null, device.getName(),
                "DELETED", device.statusLine(), null);
    }

    /** Toggle primary power (or LOCK/UNLOCK for locks). Throws when offline. */
    public void togglePower(Device device) throws SQLException {
        Device d = require(device.getId());
        setPower(d, !d.isOn(), ActivityLogRepository.ACTOR_USER);
    }

    /** Set power to an explicit target, logging the change. */
    public void setPower(Device device, boolean target, String actor) throws SQLException {
        Device d = require(device.getId());
        if (!d.isOnline()) {
            throw new IllegalStateException(
                    d.getName() + " is offline - control commands cannot be delivered.");
        }
        if (d.isOn() == target) {
            return;
        }
        String before = stateLabel(d);
        d.setPower(target);
        deviceRepository.updatePowerState(d);
        activityLogRepository.insert(currentUserId, d.getId(), d.getName(),
                powerAction(d, d.isOn()), before, stateLabel(d), actor);
    }

    public void applySetting(Device device, String key, String value) throws SQLException {
        applySetting(device, key, value, ActivityLogRepository.ACTOR_USER);
    }

    public void applySetting(Device device, String key, String value, String actor)
            throws SQLException {
        Device d = require(device.getId());
        if (!d.isOnline()) {
            throw new IllegalStateException(
                    d.getName() + " is offline - settings cannot be delivered.");
        }
        String before = d.statusLine();
        d.setSetting(key, value);
        deviceRepository.updateSettings(d);
        String after = d.statusLine();
        if (!before.equals(after)) {
            activityLogRepository.insert(currentUserId, d.getId(), d.getName(),
                    "SET_" + key.toUpperCase(), before, after, actor);
        }
    }

    /** Simulated connectivity change (the app has no real radios). */
    public void setConnection(Device device, boolean online) throws SQLException {
        Device d = require(device.getId());
        if (d.isOnline() == online) {
            return;
        }
        d.setOnline(online);
        deviceRepository.updateConnectionStatus(d);
        activityLogRepository.insert(currentUserId, d.getId(), d.getName(),
                online ? "CAME_ONLINE" : "WENT_OFFLINE",
                null, online ? "ONLINE" : "OFFLINE");
    }

    /* ---------- dashboard statistics ---------- */

    public long countAll() { return devices.size(); }
    public long countOnline() { return devices.stream().filter(Device::isOnline).count(); }
    public long countOffline() { return devices.size() - countOnline(); }
    public long countActive() { return devices.stream().filter(Device::isOn).count(); }

    public Map<DeviceType, Long> countByType() {
        Map<DeviceType, Long> map = new java.util.EnumMap<>(DeviceType.class);
        for (DeviceType t : DeviceType.values()) {
            map.put(t, devices.stream().filter(d -> d.getDeviceType() == t).count());
        }
        return map;
    }

    /* ---------- helpers ---------- */

    private Device require(int deviceId) {
        return devices.stream()
                .filter(d -> d.getId() == deviceId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown device id " + deviceId));
    }

    private String stateLabel(Device d) {
        return d.isOn() ? d.onLabel() : d.offLabel();
    }

    private String powerAction(Device d, boolean on) {
        if (d.getDeviceType() == DeviceType.DOOR_LOCK) {
            return on ? "LOCKED" : "UNLOCKED";
        }
        return on ? "TURNED_ON" : "TURNED_OFF";
    }

    private void validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Device name must be at least 2 characters.");
        }
    }
}
