-- Run after 20261006_device_port_diary.sql. No new table is needed.
-- A NULL active_port_index lets past cultivations share the same device/port.

SET @schema_name = DATABASE();

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND COLUMN_NAME = 'ended_at');
SET @sql = IF(@has_column = 0, 'ALTER TABLE plant ADD COLUMN ended_at DATETIME(6) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND COLUMN_NAME = 'end_reason');
SET @sql = IF(@has_column = 0, 'ALTER TABLE plant ADD COLUMN end_reason VARCHAR(32) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND COLUMN_NAME = 'active_port_index');
SET @sql = IF(@has_column = 0, 'ALTER TABLE plant ADD COLUMN active_port_index INT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

UPDATE plant SET active_port_index = port_index WHERE ended_at IS NULL AND active_port_index IS NULL;
UPDATE plant SET active_port_index = NULL WHERE ended_at IS NOT NULL;

-- The old unique index may also be MySQL's supporting index for the device FK.
SET @has_device_index = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND INDEX_NAME = 'idx_plant_device_serial');
SET @sql = IF(@has_device_index = 0, 'ALTER TABLE plant ADD INDEX idx_plant_device_serial (device_serial)', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_old_unique = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND INDEX_NAME = 'uk_device_port' AND NON_UNIQUE = 0);
SET @sql = IF(@has_old_unique > 0, 'ALTER TABLE plant DROP INDEX uk_device_port', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_active_unique = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND INDEX_NAME = 'uk_device_port' AND NON_UNIQUE = 0);
SET @sql = IF(@has_active_unique = 0, 'ALTER TABLE plant ADD CONSTRAINT uk_device_port UNIQUE (device_serial, active_port_index)', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;
