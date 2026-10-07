-- Run this once before 20261006_device_port_diary.sql on databases created with
-- the old device.species_id / plant.plant_stage model. No table is added.
-- Stop if any existing plant cannot be assigned a species; do not guess one.

SET @schema_name = DATABASE();

SET @has_plant_species = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND COLUMN_NAME = 'species_id'
);
SET @sql = IF(@has_plant_species = 0,
    'ALTER TABLE `plant` ADD COLUMN `species_id` BIGINT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @has_device_species = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'device' AND COLUMN_NAME = 'species_id'
);
SET @sql = IF(@has_device_species > 0,
    'UPDATE `plant` p JOIN `device` d ON d.`serial_number` = p.`device_serial` SET p.`species_id` = d.`species_id` WHERE p.`species_id` IS NULL AND d.`species_id` IS NOT NULL',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- This fails instead of silently losing the species of an existing plant.
ALTER TABLE `plant` MODIFY COLUMN `species_id` BIGINT NOT NULL;

SET @has_plant_species_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant'
      AND COLUMN_NAME = 'species_id' AND REFERENCED_TABLE_NAME = 'species'
);
SET @sql = IF(@has_plant_species_fk = 0,
    'ALTER TABLE `plant` ADD CONSTRAINT `fk_plant_species` FOREIGN KEY (`species_id`) REFERENCES `species` (`id`)',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @has_device_port_unique = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant'
      AND INDEX_NAME = 'uk_device_port' AND NON_UNIQUE = 0
);
SET @sql = IF(@has_device_port_unique = 0,
    'ALTER TABLE `plant` ADD CONSTRAINT `uk_device_port` UNIQUE (`device_serial`, `port_index`)',
    'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @has_old_plant_stage = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'plant' AND COLUMN_NAME = 'plant_stage'
);
SET @sql = IF(@has_old_plant_stage > 0,
    'ALTER TABLE `plant` DROP COLUMN `plant_stage`', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @device_species_fk = (
    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'device'
      AND COLUMN_NAME = 'species_id' AND REFERENCED_TABLE_NAME = 'species'
    LIMIT 1
);
SET @sql = IF(@device_species_fk IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE `device` DROP FOREIGN KEY `', REPLACE(@device_species_fk, '`', '``'), '`'));
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @sql = IF(@has_device_species > 0,
    'ALTER TABLE `device` DROP COLUMN `species_id`', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
