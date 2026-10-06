-- Run once on databases with the old plant_id diary schema before starting this version.
-- No new table is created. Existing notes keep their device, port and plant name snapshot.
ALTER TABLE diary
    ADD COLUMN device_serial VARCHAR(255) NULL,
    ADD COLUMN port_index INT NULL,
    ADD COLUMN species_name VARCHAR(255) NULL,
    ADD COLUMN plant_name VARCHAR(255) NULL;

UPDATE diary d
JOIN plant p ON p.id = d.plant_id
JOIN species s ON s.id = p.species_id
SET d.device_serial = p.device_serial,
    d.port_index = p.port_index,
    d.species_name = s.name,
    d.plant_name = p.name;

ALTER TABLE diary
    MODIFY COLUMN device_serial VARCHAR(255) NOT NULL,
    MODIFY COLUMN port_index INT NOT NULL,
    DROP FOREIGN KEY FKbgbx3br3g9w08akvl3gf7x4kx,
    DROP COLUMN plant_id,
    ADD INDEX idx_diary_device_serial (device_serial),
    ADD CONSTRAINT fk_diary_device FOREIGN KEY (device_serial) REFERENCES device(serial_number);
