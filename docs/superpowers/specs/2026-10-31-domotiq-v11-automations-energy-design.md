# DOMOTIQ v1.1 — Automations & Energy Monitoring (Design)

Date: 2026-10-31
Status: approved scope — Option B, pure Swing, trigger scope C, Approach 1 (event pipeline + rule tables)

## Goals

1. **Scenes & Automations** — users define rules that control devices automatically:
   - **Scene**: manual one-tap run of an ordered list of device actions.
   - **Schedule**: fires at a wall-clock time on chosen weekdays (optional active window).
   - **Sensor**: fires when a simulated sensor reading crosses a threshold
     (`> value`, `< value`) or reports detection (`MOTION DETECTED` / `ALERT: smoke`).
2. **Energy monitoring** — per-device estimated consumption accumulated from
   simulated uptime, persisted daily, visualised with hand-painted Swing charts
   (no JFreeChart, no HTML).

Non-goals: device-state triggers (a light turning on firing a rule), real
hardware, multiple users sharing rules. Everything remains per-user, simulated.

## 1. Database (additions to `database/init.sql`, plus one-time `database/migration_v1.1.sql`)

```sql
automations (
  automation_id INT UNSIGNED PK AI,
  user_id INT UNSIGNED FK -> users CASCADE,
  name VARCHAR(80) NOT NULL,
  trigger_type ENUM('SCENE','SCHEDULE','SENSOR') NOT NULL,
  enabled TINYINT(1) DEFAULT 1,
  -- SENSOR trigger
  sensor_device_id INT UNSIGNED NULL FK -> devices SET NULL,
  sensor_op ENUM('GT','LT','DETECTED') NULL,
  sensor_value DECIMAL(8,2) NULL,
  -- SCHEDULE trigger
  fire_time TIME NULL,
  days_mask CHAR(7) NULL COMMENT 'Mon..Sun as 1/0',
  active_start TIME NULL, active_end TIME NULL,
  -- shared
  cooldown_minutes INT UNSIGNED DEFAULT 10,
  last_fired_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)

automation_actions (
  action_id PK, automation_id FK -> automations CASCADE,
  sort_order INT NOT NULL,
  device_id INT UNSIGNED NOT NULL FK -> devices CASCADE,
  action_kind ENUM('POWER','SET') NOT NULL,
  power_target TINYINT(1) NULL,      -- for POWER
  setting_key VARCHAR(50) NULL,      -- for SET
  setting_value VARCHAR(100) NULL
)

energy_readings (
  reading_id PK, device_id FK -> devices CASCADE,
  user_id FK -> users CASCADE,
  reading_date DATE NOT NULL,
  kwh DECIMAL(10,4) NOT NULL DEFAULT 0,
  UNIQUE (device_id, reading_date)
)

activity_logs ALTER: + actor ENUM('USER','AUTOMATION') DEFAULT 'USER'
```

## 2. Rule engine (`com.domotiq.automation`)

- `TriggerType` enum; `Automation` + `AutomationAction` model classes mirroring tables.
- `RuleEngine` owns the loaded automations for the logged-in user and exposes:
  - `onSensorReading(SmartSensor)` — called from `DeviceSimulator` tick.
  - `onMinuteTick(LocalDateTime now)` — called from a 60 s `javax.swing.Timer` in `MainFrame`.
  - `runScene(automationId)` — called from the Automations panel.
- Matching: trigger type + enabled + sensor condition parse (numeric comparison of
  leading double in reading; DETECTED = reading contains "DETECTED"/"ALERT") +
  time/day/window checks. Cooldown: skip if `now < last_fired_at + cooldown_minutes`.
- Execution: ordered actions run through `DeviceController` (new
  `setPower(Device, boolean, actor)` / existing `applySetting`) so every automated
  change is persisted and appears in the activity log with `actor='AUTOMATION'`.
  Offline devices: action skipped, rule still marked fired, error surfaced via
  the panel status label. On success `last_fired_at` updated in memory + DB.
- `AutomationRepository` (find/insert/update/delete/setEnabled/touchLastFired/
  countFiredToday), `AutomationController` validates rules before persisting.

## 3. Energy model

- `Device.nominalWatts()` abstract; per category: LIGHT 15, FAN 60, AC 1200,
  DOOR_LOCK 3, CAMERA 12, SENSOR 2.
- `DeviceSimulator` tick (4 s): for each online + ON device accumulate
  `watts * 4s` into an in-memory `EnergyTracker`; `onMinuteTick` flushes daily
  totals via `EnergyRepository.addKwh(deviceId, date, kwh)` (upsert on the
  unique key). Values are estimates from simulated uptime — labelled as such.
- `EnergyController`: last-7-days totals, current-month total, per-device month
  breakdown. `EnergyRepository` queries with `SUM/GROUP BY`.

## 4. GUI

- Sidebar: **Automations**, **Energy** cards in `MainFrame` CardLayout.
- `AutomationsPanel`: rule cards (name, trigger summary, enabled checkbox,
  cooldown, last fired), Run-now button for scenes, Edit/Delete,
  "+ New scene" / "+ New automation" buttons; fires through RuleEngine on EDT.
- `AutomationDialog`: name, trigger-kind combo driving a `CardLayout` condition
  panel (SCENE: none; SCHEDULE: time field + weekday checkboxes + cooldown;
  SENSOR: sensor-device combo + op combo + value field + cooldown), then an
  action list builder (device combo, POWER ON/OFF or SET <key>=<value> from the
  device's `settingDefinitions()`), reorder/add/remove rows. Persists via
  `AutomationController`, reloads `RuleEngine`.
- `EnergyPanel`: stat row (this month kWh, today kWh, per-device average),
  `EnergyChartPanel` — custom `paintComponent` 7-day bar chart (grid lines,
  day labels, value labels, Theme colors), device kWh `JTable` for the month.
- Dashboard: 5th stat card "Automations fired today" from
  `AutomationRepository.countFiredToday`.

## 5. Testing / verification

- `mvn package` compiles clean; app boots against MySQL.
- Manual matrix: scene runs all actions & logs AUTOMATION; sensor rule fires on
  threshold crossing and is blocked during cooldown; schedule rule fires at its
  minute on the right weekday; energy table grows ~1 kWh/AC-hour; chart renders
  in both themes; deleting a device removes its rules' actions (CASCADE).
