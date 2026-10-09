-- Run on an existing MySQL database before starting the updated BE.
-- Back up the database first. The last two statements remove retired tables.

-- Hibernate ddl-auto=update may have created this column already.
SET @file_url_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file' AND COLUMN_NAME = 'file_url'
);
SET @add_file_url = IF(@file_url_exists = 0,
    'ALTER TABLE `file` ADD COLUMN `file_url` VARCHAR(1024) NULL',
    'SELECT 1');
PREPARE add_file_url_stmt FROM @add_file_url;
EXECUTE add_file_url_stmt;
DEALLOCATE PREPARE add_file_url_stmt;

-- Article uploads previously wrote both File and Image. Reuse the File row.
UPDATE `file` f
LEFT JOIN `image` i
  ON i.article_id = f.article_id
 AND i.diary_id IS NULL
 AND SUBSTRING_INDEX(i.img_url, '/', -1) = f.stored_file_name
SET f.file_url = CASE
    WHEN i.img_url IS NULL OR i.img_url LIKE '/api/files/%'
      THEN CONCAT('/uploads/', f.stored_file_name)
    ELSE i.img_url
END;

-- Diary images, and any article images without a File row, need a File row.
INSERT INTO `file` (
    `created_at`, `updated_at`, `original_file_name`, `stored_file_name`,
    `file_path`, `file_url`, `article_id`, `diary_id`
)
SELECT i.created_at, i.updated_at,
       SUBSTRING_INDEX(i.img_url, '/', -1),
       SUBSTRING_INDEX(i.img_url, '/', -1),
       i.img_url,
       CASE WHEN i.img_url LIKE '/api/files/%'
            THEN CONCAT('/uploads/', SUBSTRING_INDEX(i.img_url, '/', -1))
            ELSE i.img_url END,
       i.article_id, i.diary_id
FROM `image` i
WHERE NOT EXISTS (
    SELECT 1 FROM `file` f
    WHERE f.article_id <=> i.article_id
      AND f.diary_id <=> i.diary_id
      AND f.stored_file_name = SUBSTRING_INDEX(i.img_url, '/', -1)
);

ALTER TABLE `file` MODIFY COLUMN `file_url` VARCHAR(1024) NOT NULL;

-- Verify every Image has a matching File before dropping the old table.
DELIMITER //
CREATE PROCEDURE `assert_image_migrated`()
BEGIN
    IF EXISTS (
        SELECT 1 FROM `image` i
        WHERE NOT EXISTS (
            SELECT 1 FROM `file` f
            WHERE f.article_id <=> i.article_id
              AND f.diary_id <=> i.diary_id
              AND f.stored_file_name = SUBSTRING_INDEX(i.img_url, '/', -1)
        )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Image rows remain without File rows';
    END IF;
END//
DELIMITER ;

CALL `assert_image_migrated`();
DROP PROCEDURE `assert_image_migrated`;

DROP TABLE `image`;
DROP TABLE `sensor_anomaly`;
