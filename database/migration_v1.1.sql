-- ============================================================
-- DOMOTIQ v1.1 migration  (run ONCE on an existing v1.0 database)
--   mysql -u <username> -p domotiq < database/migration_v1.1.sql
-- Fresh installs don't need this - database/init.sql already
-- contains everything below.
-- The ALTER at the end is not idempotent: if it errors with
-- "Duplicate column name 'actor'", the migration already ran.
-- ============================================================

USE domotiq;

CREATE TABLE IF NOT EXISTS automations (
  automation_id     INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  user_id           INT UNSIGNED  NOT NULL,
  name              VARCHAR(80)   NOT NULL,
  trigger_type      ENUM('SCENE','SCHEDULE','SENSOR') NOT NULL,
  enabled           TINYINT(1)    NOT NULL DEFAULT 1,
  sensor_device_id  INT UNSIGNED  NULL,
  sensor_op         ENUM('GT','LT','DETECTED') NULL,
  sensor_value      DECIMAL(8,2)  NULL,
  fire_time         TIME          NULL,
  days_mask         CHAR(7)       NULL COMMENT 'Mon..Sun as 1/0 characters',
  active_start      TIME          NULL,
  active_end        TIME          NULL,
  cooldown_minutes  INT UNSIGNED  NOT NULL DEFAULT 10,
  last_fired_at     DATETIME      NULL,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_automations PRIMARY KEY (automation_id),
  CONSTRAINT fk_automations_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_automations_sensor
    FOREIGN KEY (sensor_device_id) REFERENCES devices (device_id)
    ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS automation_actions (
  action_id       INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  automation_id   INT UNSIGNED  NOT NULL,
  sort_order      INT           NOT NULL,
  device_id       INT UNSIGNED  NOT NULL,
  action_kind     ENUM('POWER','SET') NOT NULL,
  power_target    TINYINT(1)    NULL,
  setting_key     VARCHAR(50)   NULL,
  setting_value   VARCHAR(100)  NULL,
  CONSTRAINT pk_automation_actions PRIMARY KEY (action_id),
  CONSTRAINT fk_actions_automation
    FOREIGN KEY (automation_id) REFERENCES automations (automation_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_actions_device
    FOREIGN KEY (device_id) REFERENCES devices (device_id)
    ON DELETE CASCADE,
  INDEX idx_actions_automation (automation_id, sort_order)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS energy_readings (
  reading_id    INT UNSIGNED   NOT NULL AUTO_INCREMENT,
  device_id     INT UNSIGNED   NOT NULL,
  user_id       INT UNSIGNED   NOT NULL,
  reading_date  DATE           NOT NULL,
  kwh           DECIMAL(10,4)  NOT NULL DEFAULT 0,
  CONSTRAINT pk_energy_readings PRIMARY KEY (reading_id),
  CONSTRAINT uq_energy_device_date UNIQUE (device_id, reading_date),
  CONSTRAINT fk_energy_device
    FOREIGN KEY (device_id) REFERENCES devices (device_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_energy_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE,
  INDEX idx_energy_user_date (user_id, reading_date)
) ENGINE = InnoDB;

ALTER TABLE activity_logs
  ADD COLUMN actor ENUM('USER','AUTOMATION') NOT NULL DEFAULT 'USER'
  AFTER performed_at;
