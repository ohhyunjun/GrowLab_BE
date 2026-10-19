-- Additive migration for an operating GrowLab RDS (MySQL 8.4).
-- Stop BE writes, take a restorable RDS snapshot and logical backup, then run this.
-- It does not delete/update any existing photo, plant, prediction or sensor row.
-- Existing photos intentionally keep NULL plant_id/captured_at/observation_id:
-- a device+port+date match is not proof of cultivation identity.
-- Each step can be rerun after a partially completed DDL migration.

SET @add_photo_plant_id = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo' AND COLUMN_NAME = 'plant_id') = 0,
    'ALTER TABLE `photo` ADD COLUMN `plant_id` BIGINT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @add_photo_plant_id;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @add_photo_captured_at = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo' AND COLUMN_NAME = 'captured_at') = 0,
    'ALTER TABLE `photo` ADD COLUMN `captured_at` DATETIME(6) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @add_photo_captured_at;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @add_photo_observation_id = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo' AND COLUMN_NAME = 'observation_id') = 0,
    'ALTER TABLE `photo` ADD COLUMN `observation_id` VARCHAR(36) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @add_photo_observation_id;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- MySQL permits many NULLs in this composite unique index, so legacy rows remain valid.
SET @add_photo_observation_key = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo'
       AND INDEX_NAME = 'uk_photo_observation') = 0,
    'ALTER TABLE `photo` ADD UNIQUE INDEX `uk_photo_observation` (`device_serial`,`observation_id`)',
    'SELECT 1');
PREPARE migration_stmt FROM @add_photo_observation_key;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @add_photo_plant_captured_key = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo'
       AND INDEX_NAME = 'idx_photo_plant_captured') = 0,
    'ALTER TABLE `photo` ADD INDEX `idx_photo_plant_captured` (`plant_id`,`captured_at`)',
    'SELECT 1');
PREPARE migration_stmt FROM @add_photo_plant_captured_key;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

CREATE TABLE IF NOT EXISTS `harvest_prediction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `plant_id` BIGINT NOT NULL,
    `device_serial` VARCHAR(255) NOT NULL,
    `observation_id` VARCHAR(36) NOT NULL,
    `photo_id` BIGINT NOT NULL,
    `captured_at` DATETIME(6) NOT NULL,
    `predicted_at` DATETIME(6) NOT NULL,
    `received_at` DATETIME(6) NOT NULL,
    `model_version` VARCHAR(64) NOT NULL,
    `feature_version` VARCHAR(64) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `anchor` VARCHAR(16) NULL,
    `anchor_at` DATETIME(6) NULL,
    `remaining_days` DOUBLE NULL,
    `expected_at` DATETIME(6) NULL,
    `features_json` TEXT NULL,
    CONSTRAINT `uk_harvest_observation` UNIQUE (`device_serial`, `observation_id`),
    INDEX `idx_harvest_plant_captured` (`plant_id`, `captured_at`),
    CONSTRAINT `fk_harvest_plant` FOREIGN KEY (`plant_id`) REFERENCES `plant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
