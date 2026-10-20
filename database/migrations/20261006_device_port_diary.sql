-- Run after 20261005_device_port_species.sql. Keep plant_id: a diary belongs
-- to one cultivation cycle, while device/port/name remain display snapshots.
-- This also accepts a database where the older port-only migration was run.

SET @schema_name = DATABASE();

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'diary' AND COLUMN_NAME = 'device_serial');
SET @sql = IF(@has_column = 0, 'ALTER TABLE diary ADD COLUMN device_serial VARCHAR(255) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'diary' AND COLUMN_NAME = 'port_index');
SET @sql = IF(@has_column = 0, 'ALTER TABLE diary ADD COLUMN port_index INT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'diary' AND COLUMN_NAME = 'species_name');
SET @sql = IF(@has_column = 0, 'ALTER TABLE diary ADD COLUMN species_name VARCHAR(255) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'diary' AND COLUMN_NAME = 'plant_name');
SET @sql = IF(@has_column = 0, 'ALTER TABLE diary ADD COLUMN plant_name VARCHAR(255) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'diary' AND COLUMN_NAME = 'plant_id');
SET @sql = IF(@has_column = 0, 'ALTER TABLE diary ADD COLUMN plant_id BIGINT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

UPDATE diary d
JOIN plant p ON p.id = d.plant_id
JOIN species s ON s.id = p.species_id
SET d.device_serial = COALESCE(d.device_serial, p.device_serial),
    d.port_index = COALESCE(d.port_index, p.port_index),
    d.species_name = COALESCE(d.species_name, s.name),
    d.plant_name = COALESCE(d.plant_name, p.name);

-- A diary whose plant was already hard-deleted keeps its existing snapshots.
-- Do not guess its plant_id from the current occupant of the same port.
ALTER TABLE diary
    MODIFY COLUMN device_serial VARCHAR(255) NOT NULL,
    MODIFY COLUMN port_index INT NOT NULL;

SET @has_fk = (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'diary' AND COLUMN_NAME = 'plant_id' AND REFERENCED_TABLE_NAME = 'plant');
SET @sql = IF(@has_fk = 0, 'ALTER TABLE diary ADD CONSTRAINT fk_diary_plant FOREIGN KEY (plant_id) REFERENCES plant(id)', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @has_fk = (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'diary' AND COLUMN_NAME = 'device_serial' AND REFERENCED_TABLE_NAME = 'device');
SET @sql = IF(@has_fk = 0, 'ALTER TABLE diary ADD CONSTRAINT fk_diary_device FOREIGN KEY (device_serial) REFERENCES device(serial_number)', 'SELECT 1');
PREPARE migration_stmt FROM @sql; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;
