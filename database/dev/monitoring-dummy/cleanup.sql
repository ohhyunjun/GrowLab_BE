-- 더미 데이터 되돌리기 (로컬 DB 전용). 더미 기기와 확인용 계정만 지운다.
SET NAMES utf8mb4;
START TRANSACTION;
DELETE FROM prediction WHERE plant_id IN (SELECT id FROM plant WHERE device_serial = 'GROWLAB-DUMMY1');
DELETE FROM file WHERE diary_id IN (SELECT id FROM diary WHERE device_serial = 'GROWLAB-DUMMY1');
DELETE FROM diary WHERE device_serial = 'GROWLAB-DUMMY1';
DELETE FROM notice WHERE device_serial = 'GROWLAB-DUMMY1';
DELETE FROM photo WHERE device_serial = 'GROWLAB-DUMMY1';
DELETE FROM sensor_log WHERE serial_number = 'GROWLAB-DUMMY1';
DELETE FROM plant WHERE device_serial = 'GROWLAB-DUMMY1';
DELETE FROM device WHERE serial_number = 'GROWLAB-DUMMY1';
DELETE FROM email_verification WHERE email = 'growlab-dummy@example.test';
DELETE FROM user WHERE username = 'growlab_dummy';
COMMIT;
