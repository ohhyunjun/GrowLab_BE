-- Run against an existing local MySQL database if Hibernate ddl-auto=update has not added this nullable column.
-- Safe to rerun: existing rows and an existing green_area_ratio column are preserved.
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
