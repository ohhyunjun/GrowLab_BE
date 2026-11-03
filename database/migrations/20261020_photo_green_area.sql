-- Additive change for the operating GrowLab MySQL database after 20261018_edge_harvest.sql.
-- Test on a restored copy first. Safe to rerun; existing photo rows and values are preserved.
SET @green_area_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'photo'
      AND COLUMN_NAME = 'green_area_ratio'
);
SET @add_green_area_column = IF(
    @green_area_column_exists = 0,
    'ALTER TABLE photo ADD COLUMN green_area_ratio DOUBLE NULL',
    'SELECT 1'
);
PREPARE add_green_area_column_stmt FROM @add_green_area_column;
EXECUTE add_green_area_column_stmt;
DEALLOCATE PREPARE add_green_area_column_stmt;
