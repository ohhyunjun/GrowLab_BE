-- Read-only checks for the existing AWS MySQL database. Run before any ALTER.
-- Compare the result with the actual schema; do not assume the 2026-09-22 dump
-- still describes RDS. TABLE_ROWS is approximate for InnoDB.

SELECT DATABASE() AS database_name, NOW() AS checked_at;

SELECT table_name, table_rows AS estimated_rows
FROM information_schema.TABLES
WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
ORDER BY table_name;

SELECT table_name, column_name, column_type, is_nullable, column_default
FROM information_schema.COLUMNS
WHERE table_schema = DATABASE()
ORDER BY table_name, ordinal_position;

SELECT table_name, constraint_name, column_name,
       referenced_table_name, referenced_column_name
FROM information_schema.KEY_COLUMN_USAGE
WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name, ordinal_position;

-- The following exact checks expect the old AWS tables. A missing table or
-- column means the live schema has diverged; stop and adapt the migration.
SELECT 'device' AS table_name, COUNT(*) AS rows_count FROM device
UNION ALL SELECT 'species', COUNT(*) FROM species
UNION ALL SELECT 'plant', COUNT(*) FROM plant
UNION ALL SELECT 'diary', COUNT(*) FROM diary
UNION ALL SELECT 'file', COUNT(*) FROM `file`
UNION ALL SELECT 'image', COUNT(*) FROM `image`
UNION ALL SELECT 'sensor_anomaly', COUNT(*) FROM sensor_anomaly
UNION ALL SELECT 'photo', COUNT(*) FROM photo
UNION ALL SELECT 'prediction', COUNT(*) FROM prediction
UNION ALL SELECT 'notice', COUNT(*) FROM notice
UNION ALL SELECT 'sensor_log', COUNT(*) FROM sensor_log;

SELECT p.id AS plant_id, p.device_serial, p.port_index, d.species_id
FROM plant p LEFT JOIN device d ON d.serial_number = p.device_serial
WHERE d.species_id IS NULL;

SELECT d.id AS diary_id, d.plant_id
FROM diary d LEFT JOIN plant p ON p.id = d.plant_id
WHERE p.id IS NULL;

SELECT p.id AS plant_id, p.device_serial, p.port_index
FROM plant p
WHERE p.port_index NOT BETWEEN 0 AND 7;

SELECT serial_number, port_status
FROM device
WHERE port_status IS NULL OR CHAR_LENGTH(port_status) <> 8;

SELECT COUNT(*) AS s3_file_paths
FROM `file`
WHERE file_path LIKE 'https://%' OR file_path LIKE 'http://%';

SELECT COUNT(*) AS s3_image_urls
FROM `image`
WHERE img_url LIKE 'https://%' OR img_url LIKE 'http://%';
