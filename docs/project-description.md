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
event is logged with device name, action, previous status, new status, actor
(you or automation) and timestamp, browsable in the History screen and on the
dashboard.

### 3.7 Scenes & automations (v1.1)
Per-user rules executed by an in-app event pipeline:

| Trigger  | Behaviour |
|----------|-----------|
| Scene    | Manual only — press *Run now* to execute an ordered list of device steps (Turn ON/OFF/LOCK/UNLOCK, or set any device setting). |
| Schedule | Fires at a chosen HH:mm on selected weekdays, optionally limited to an active time window; missed runs are not replayed after the app restarts more than 10 minutes late. |
| Sensor   | Watches one simulated sensor's live telemetry; fires when the reading is above/below a threshold or reports detection (motion/smoke). |

Rules can be enabled/disabled, edited and deleted; each carries a cooldown
(default 10 min) so flapping readings cannot spam devices. Every automated
change goes through the normal controller path, is persisted, and is logged
with `actor = AUTOMATION`.

### 3.8 Energy monitoring (v1.1)
Estimated consumption derived from simulated uptime: each device category has
a nameplate wattage, the simulator accumulates `watts × seconds ON`, and a
minute-level flush upserts daily kWh into `energy_readings`. The Energy screen
shows a hand-painted (pure `Graphics2D`) 7-day bar chart, per-device monthly
totals, and derived run-hours. Estimates exist only while the app runs — no
physical meters are involved.

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
  telemetry and uptime energy deltas; the seam where a real IoT client would
  plug in.
- **`RuleEngine` (v1.1)** — consumes three event kinds (sensor readings,
  minute ticks, manual scene runs), matches enabled automations, enforces
  cooldowns, and executes steps through `DeviceController` with
  `actor = AUTOMATION`.
- **`EnergyTracker` (v1.1)** — accumulates kWh deltas in memory and flushes
  them once a minute via `EnergyRepository` (`INSERT … ON DUPLICATE KEY
  UPDATE`).
- **`AutomationController` / `EnergyController`** — validation and
  aggregation for the two v1.1 screens, same layering as everything else.

### How to add a new device category

1. Create `model/devices/SmartSpeaker.java extends Device`, declaring its
   `settingDefinitions()`.
2. Add `SPEAKER("Smart Speaker")` to `DeviceType` and one case in
   `DeviceFactory`.
3. Add the value to the `device_type` ENUM in MySQL.

No GUI or repository changes are required — controls and storage adapt
automatically.

## 5. Database Design

Schema `domotiq`, eight relations:

| Table             | Purpose                                   | Keys |
|-------------------|-------------------------------------------|------|
| `users`           | accounts, PBKDF2 hash + salt              | PK user_id; unique username, email |
| `rooms`           | per-user room catalogue                   | PK room_id; FK user_id → users (CASCADE); unique (user_id, room_name) |
| `devices`         | device records incl. power + connection   | PK device_id; FK user_id (CASCADE), FK room_id (SET NULL) |
| `device_settings` | generic key/value settings per device     | PK setting_id; FK device_id (CASCADE); unique (device_id, setting_key) |
| `activity_logs`   | immutable audit trail incl. actor         | PK log_id; FK user_id (CASCADE), FK device_id (SET NULL) |
| `automations`     | scene/schedule/sensor rule definitions (v1.1) | PK automation_id; FK user_id (CASCADE), FK sensor device (SET NULL) |
| `automation_actions` | ordered device steps per rule (v1.1)   | PK action_id; FK automation_id (CASCADE), FK device_id (CASCADE) |
| `energy_readings` | daily estimated kWh per device (v1.1)    | PK reading_id; FK device_id (CASCADE), FK user_id (CASCADE); unique (device_id, reading_date) |

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
├── model (+ .devices)   domain objects, Device hierarchy, Automation rules
├── factory              DeviceFactory
├── repository           JDBC data access (6 classes)
├── controller           business orchestration (6 classes)
├── automation           RuleEngine (event pipeline, cooldowns)
├── simulation           DeviceSimulator + EnergyTracker
└── gui                  18 Swing classes incl. Theme/ThemeManager,
                         Automations/Energy screens, hand-painted chart
                         (dark + light palettes, responsive layouts)
```
