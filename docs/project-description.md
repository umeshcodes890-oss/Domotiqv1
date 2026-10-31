# DOMOTIQ — Project Description

**Version:** 1.0 · **Status:** Complete desktop release · **Type:** Simulated IoT management system

## 1. Overview

DOMOTIQ is a centralized smart home management desktop application. Users
register an account, organise simulated IoT devices into rooms, monitor their
status, switch them on/off (or lock/unlock), adjust category-specific
settings, and review a complete audit trail of every action.

> **Important:** all devices in v1 are *simulated*. State lives in the local
> MySQL database and in memory; no physical hardware or network radios are
> involved. The architecture is deliberately shaped so a real IoT transport
> (MQTT, HTTP, BLE) can replace the simulation layer later without rewriting
> the application.

## 2. Technology Stack

| Concern        | Choice                                            |
|----------------|---------------------------------------------------|
| Language       | Java 21+ (developed on JDK 26)                    |
| GUI            | Java Swing (pure desktop — no HTML/CSS/JS)        |
| Database       | MySQL 8.x                                         |
| Data access    | JDBC with PreparedStatements (no ORM)             |
| Build          | Apache Maven, shaded executable jar               |
| Auth security  | PBKDF2-HMAC-SHA256, per-user salt, constant-time compare |

Deliberate exclusions: **no Hibernate, no Spring Boot, no web frontend.**

## 3. Feature Summary

### 3.1 User authentication
Registration with validation, login, logout, hashed passwords, and complete
per-user data isolation (every query is scoped by `user_id`).

### 3.2 Dashboard
Live counters (registered / online / offline / active devices), a category
breakdown, quick ON/OFF controls for up to nine devices, and a recent-activity
feed. Updates every 4 seconds from the device simulator.

### 3.3 Device management
Add, edit, delete, search, and filter devices by category and room. Each
device record carries: id, name, type, room assignment, connection status,
operating (power) state, and date added.

### 3.4 Device categories and their controls

| Category        | Controls                                              |
|-----------------|-------------------------------------------------------|
| Smart Lights    | ON/OFF, brightness 1–100 %, colour temperature        |
| Smart Fans      | ON/OFF, speed 1–3, oscillation                        |
| Smart AC        | ON/OFF, target temperature 16–30 °C, mode (COOL/HEAT/FAN/DRY/AUTO) |
| Smart Door Lock | LOCK/UNLOCK, auto-lock timer                          |
| Smart Camera    | MONITORING/STANDBY, resolution, night vision, recording |
| Smart Sensors   | ARMED/SILENT, kind (temperature/humidity/motion/smoke), live simulated readings |

### 3.5 Room management
Create, rename, describe and delete rooms; assign devices; view all devices
in a selected room. Deleting a room never deletes devices — they become
unassigned (foreign key `ON DELETE SET NULL`).

### 3.6 Activity history
Every registration, power change, setting change, deletion and connectivity
event is logged with device name, action, previous status, new status and
timestamp, browsable in the History screen and on the dashboard.

## 4. Architecture

Layered MVC-style design:

```
Swing GUI (com.domotiq.gui)
      │ calls
Controllers (com.domotiq.controller)   validation + orchestration
      │ use
Repositories (com.domotiq.repository)  JDBC / PreparedStatements only
      │ against
MySQL (domotiq schema)
```

Key object-oriented pieces:

- **`Device` (abstract)** — common contract: identity, power, connectivity,
  settings map, and abstract hooks (`onLabel`, `statusLine`,
  `settingDefinitions`).
- **`SettingDefinition`** — each category *declares* its own controls
  (slider / combo / toggle). The dialog and the persistence layer render and
  store them generically.
- **`DeviceFactory`** — the only place mapping `DeviceType` → concrete class.
- **`DeviceController`** — single funnel for every mutation: validate →
  mutate model → write-through to MySQL → record activity log.
- **`DeviceSimulator`** — `javax.swing.Timer` producing drifting sensor
  telemetry; the seam where a real IoT client would plug in.

### How to add a new device category

1. Create `model/devices/SmartSpeaker.java extends Device`, declaring its
   `settingDefinitions()`.
2. Add `SPEAKER("Smart Speaker")` to `DeviceType` and one case in
   `DeviceFactory`.
3. Add the value to the `device_type` ENUM in MySQL.

No GUI or repository changes are required — controls and storage adapt
automatically.

## 5. Database Design

Schema `domotiq`, five relations:

| Table             | Purpose                                   | Keys |
|-------------------|-------------------------------------------|------|
| `users`           | accounts, PBKDF2 hash + salt              | PK user_id; unique username, email |
| `rooms`           | per-user room catalogue                   | PK room_id; FK user_id → users (CASCADE); unique (user_id, room_name) |
| `devices`         | device records incl. power + connection   | PK device_id; FK user_id (CASCADE), FK room_id (SET NULL) |
| `device_settings` | generic key/value settings per device     | PK setting_id; FK device_id (CASCADE); unique (device_id, setting_key) |
| `activity_logs`   | immutable audit trail                     | PK log_id; FK user_id (CASCADE), FK device_id (SET NULL) |

The key/value `device_settings` table is what keeps the schema stable while
device categories evolve.

## 6. Configuration & Security

- Credentials resolve from environment variables (`DOMOTIQ_DB_URL/USER/
  PASSWORD`) or `config/domotiq.properties`; neither is ever committed.
- Passwords are never stored in plaintext; verification is constant-time.
- All SQL uses `PreparedStatement` (no string-concatenation injection paths).
- Multi-statement device/settings writes run in explicit transactions.

## 7. Module Map

```
com.domotiq
├── Main                 startup: DB check, then login window
├── config               DatabaseConfig
├── db                   DatabaseConnection
├── util                 PasswordHasher
├── model (+ .devices)   domain objects, Device hierarchy
├── factory              DeviceFactory
├── repository           JDBC data access (4 classes)
├── controller           business orchestration (4 classes)
├── simulation           DeviceSimulator (live telemetry)
└── gui                  13 Swing classes incl. Theme/ThemeManager
                         (dark + light palettes, responsive layouts)
```
