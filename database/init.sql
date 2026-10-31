-- ============================================================
-- DOMOTIQ - Smart Home Automation System
-- MySQL initialization script
-- Run with:  mysql -u <username> -p < database/init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS domotiq
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE domotiq;

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  user_id         INT UNSIGNED     NOT NULL AUTO_INCREMENT,
  username        VARCHAR(50)      NOT NULL,
  full_name       VARCHAR(100)     NOT NULL,
  email           VARCHAR(120)     NOT NULL,
  password_hash   CHAR(64)         NOT NULL COMMENT 'hex of PBKDF2 derived key',
  password_salt   CHAR(64)         NOT NULL COMMENT 'hex random salt',
  created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_users PRIMARY KEY (user_id),
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email    UNIQUE (email)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- rooms
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
  room_id     INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  user_id     INT UNSIGNED  NOT NULL,
  room_name   VARCHAR(80)   NOT NULL,
  description VARCHAR(255)  NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_rooms PRIMARY KEY (room_id),
  CONSTRAINT uq_rooms_user_name UNIQUE (user_id, room_name),
  CONSTRAINT fk_rooms_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- devices
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devices (
  device_id         INT UNSIGNED   NOT NULL AUTO_INCREMENT,
  user_id           INT UNSIGNED   NOT NULL,
  room_id           INT UNSIGNED   NULL,
  device_name       VARCHAR(100)   NOT NULL,
  device_type       ENUM('LIGHT','FAN','AC','DOOR_LOCK','CAMERA','SENSOR') NOT NULL,
  power_state       TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '1 = ON / LOCKED where applicable',
  connection_status ENUM('ONLINE','OFFLINE') NOT NULL DEFAULT 'ONLINE',
  date_added        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_devices PRIMARY KEY (device_id),
  CONSTRAINT fk_devices_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_devices_room
    FOREIGN KEY (room_id) REFERENCES rooms (room_id)
    ON DELETE SET NULL,
  INDEX idx_devices_user_type (user_id, device_type)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- device_settings
-- Generic key/value store so new device categories never need
-- schema changes. e.g. LIGHT -> brightness=80, AC -> target_temp=22
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS device_settings (
  setting_id    INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  device_id     INT UNSIGNED  NOT NULL,
  setting_key   VARCHAR(50)   NOT NULL,
  setting_value VARCHAR(100)  NOT NULL,
  CONSTRAINT pk_device_settings PRIMARY KEY (setting_id),
  CONSTRAINT uq_device_setting UNIQUE (device_id, setting_key),
  CONSTRAINT fk_settings_device
    FOREIGN KEY (device_id) REFERENCES devices (device_id)
    ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- activity_logs
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_logs (
  log_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id         INT UNSIGNED    NOT NULL,
  device_id       INT UNSIGNED    NULL COMMENT 'NULL for non-device events',
  device_name     VARCHAR(100)    NOT NULL,
  action          VARCHAR(60)     NOT NULL COMMENT 'e.g. TURNED_ON, SET_BRIGHTNESS',
  previous_status VARCHAR(120)    NULL,
  new_status      VARCHAR(120)    NULL,
  performed_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_activity_logs PRIMARY KEY (log_id),
  CONSTRAINT fk_logs_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_logs_device
    FOREIGN KEY (device_id) REFERENCES devices (device_id)
    ON DELETE SET NULL,
  INDEX idx_logs_user_time (user_id, performed_at)
) ENGINE = InnoDB;
