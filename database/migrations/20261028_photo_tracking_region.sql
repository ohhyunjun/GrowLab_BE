-- Additive and rerunnable. No photo, cultivation or prediction row is deleted or reassigned.
-- Run before deploying the tracking build when Hibernate schema updates are disabled.
SET @tracking_region_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'photo' AND COLUMN_NAME = 'green_area_region'
);
SET @tracking_region_sql = IF(@tracking_region_exists = 0,
    'ALTER TABLE photo ADD COLUMN green_area_region VARCHAR(160) NULL', 'SELECT 1');
PREPARE tracking_region_stmt FROM @tracking_region_sql;
EXECUTE tracking_region_stmt;
DEALLOCATE PREPARE tracking_region_stmt;
