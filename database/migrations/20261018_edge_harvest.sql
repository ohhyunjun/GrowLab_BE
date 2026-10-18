-- Apply once before deploying edge photo ingestion. Back up the database first.
-- Legacy photo rows remain NULL for cultivation/capture identity.
ALTER TABLE photo
    ADD COLUMN plant_id BIGINT NULL,
    ADD COLUMN captured_at DATETIME(6) NULL,
    ADD COLUMN observation_id VARCHAR(36) NULL,
    ADD CONSTRAINT uk_photo_observation UNIQUE (device_serial, observation_id),
    ADD INDEX idx_photo_plant_captured (plant_id, captured_at);

CREATE TABLE harvest_prediction (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    device_serial VARCHAR(255) NOT NULL,
    observation_id VARCHAR(36) NOT NULL,
    photo_id BIGINT NOT NULL,
    captured_at DATETIME(6) NOT NULL,
    predicted_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    feature_version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    anchor VARCHAR(16) NULL,
    anchor_at DATETIME(6) NULL,
    remaining_days DOUBLE NULL,
    expected_at DATETIME(6) NULL,
    features_json TEXT NULL,
    CONSTRAINT uk_harvest_observation UNIQUE (device_serial, observation_id),
    INDEX idx_harvest_plant_captured (plant_id, captured_at),
    CONSTRAINT fk_harvest_plant FOREIGN KEY (plant_id) REFERENCES plant(id) ON DELETE CASCADE
);
