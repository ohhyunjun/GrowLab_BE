-- Read-only postflight. Compare the row counts with the pre-migration snapshot.
SELECT 'photo' AS table_name, COUNT(*) AS row_count FROM photo
UNION ALL SELECT 'plant', COUNT(*) FROM plant
UNION ALL SELECT 'prediction', COUNT(*) FROM prediction
UNION ALL SELECT 'harvest_prediction', COUNT(*) FROM harvest_prediction;

-- Legacy rows should retain NULL capture identity. New rows can carry it.
SELECT COUNT(*) AS total_photos,
       SUM(plant_id IS NULL) AS photos_without_explicit_plant,
       SUM(captured_at IS NULL) AS photos_without_capture_time,
       SUM(observation_id IS NULL) AS photos_without_observation_id
FROM photo;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'photo' AND COLUMN_NAME IN ('plant_id','captured_at','observation_id'))
       OR TABLE_NAME = 'harvest_prediction')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('photo','harvest_prediction')
  AND INDEX_NAME IN ('uk_photo_observation','idx_photo_plant_captured',
                     'uk_harvest_observation','idx_harvest_plant_captured')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'harvest_prediction'
  AND REFERENCED_TABLE_NAME IS NOT NULL;
