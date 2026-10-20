"""Generate dummy monitoring data for the local GrowLab DB (one device, 8 ports + finished cultivations).

Usage: python make_dummy_seed.py <output dir> <owner user id>
Writes seed.sql and cleanup.sql. Dates are relative to "now" so the scenario is always current.
"""
import datetime as dt
import json
import math
import pathlib
import random
import sys

out = pathlib.Path(sys.argv[1])
owner = int(sys.argv[2])
SERIAL = "GROWLAB-DUMMY1"
LETTUCE, OTHER = 1, 2
now = dt.datetime.now().replace(microsecond=0)
today = now.replace(hour=0, minute=0, second=0)
rng = random.Random(20261013)
SLOTS = [(5, 15), (11, 15), (17, 15), (23, 15)]
BACKGROUND = {"cls": "Growing", "conf": 0.36, "x": 0.77, "y": 0.52, "w": 0.42, "h": 0.82}


def days_ago(days, hour=9, minute=0):
    return today - dt.timedelta(days=days) + dt.timedelta(hours=hour, minutes=minute)


def q(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return repr(round(value, 6)) if isinstance(value, float) else str(value)
    if isinstance(value, dt.datetime):
        return "'" + value.strftime("%Y-%m-%d %H:%M:%S") + "'"
    return "'" + str(value).replace("'", "''") + "'"


class Plant:
    def __init__(self, key, port, species, planted, size_at, centre_at, ended=None, name=None,
                 first_age=4, disease=None, background_every=0, dark_dawn=False, cut_ages=(), only_last_day=False):
        self.key, self.port, self.species, self.planted, self.ended = key, port, species, planted, ended
        self.size_at, self.centre_at = size_at, centre_at
        self.name = name or f"더미 {key}"
        self.first_age, self.disease = first_age, disease
        self.background_every, self.dark_dawn, self.cut_ages, self.only_last_day = background_every, dark_dawn, cut_ages, only_last_day

    def age_days(self, t):
        return (t - self.planted).total_seconds() / 86400

    def stage(self):
        age = self.age_days(self.ended or now)
        return 2 if age >= 14 else 1 if age >= 7 else 0


def growth(start_size, start_age, *segments):
    """segments: (until_age, daily_ratio). Size grows by daily_ratio per day inside each segment."""
    def size_at(age):
        size, a = start_size, start_age
        for until, ratio in segments:
            if age <= a:
                break
            step = min(age, until) - a
            size *= ratio ** step
            a = min(age, until)
        return size
    return size_at


def fixed(cx, cy):
    return lambda age: (cx, cy, 1.0)


plants = [
    # 20일차 상추 넷과 질병 포트 하나: 같은 시기끼리 속도를 견준다.
    Plant("P1 보통(포트 옮김)", 0, LETTUCE, days_ago(20), growth(0.012, 4, (99, 1.10)),
          lambda age: (0.30, 0.62, 1.0) if age < 15.5 else (0.32, 0.36, 0.70), background_every=3, dark_dawn=True),
    Plant("P2 느림", 1, LETTUCE, days_ago(20, 9, 5), growth(0.012, 4, (12, 1.10), (99, 1.02)), fixed(0.28, 0.55)),
    Plant("P3 보통(잘린 날 있음)", 2, LETTUCE, days_ago(20, 9, 10), growth(0.011, 4, (99, 1.11)), fixed(0.33, 0.60), cut_ages=(16,)),
    Plant("P4 빠름", 3, LETTUCE, days_ago(20, 9, 15), growth(0.004, 4, (99, 1.22)), fixed(0.30, 0.55), background_every=2),
    # 9일차: 끝난 재배들의 같은 시기 기록과 견준다.
    Plant("P5 어린 상추", 4, LETTUCE, days_ago(9, 10), growth(0.010, 3, (99, 1.12)), fixed(0.30, 0.58), first_age=3),
    # 이틀 전 같은 날 다시 심은 포트: 관측이 하루뿐이라 대기.
    Plant("P6 갓 심음", 5, LETTUCE, days_ago(2, 10, 30), growth(0.004, 1.5, (99, 1.15)), fixed(0.31, 0.60), first_age=1.5, only_last_day=True),
    # 다른 품종: 같은 품종 기록이 없어 비교 대기.
    Plant("P7 다른 품종", 6, OTHER, days_ago(12), growth(0.010, 4, (99, 1.10)), fixed(0.31, 0.57)),
    Plant("P8 질병", 7, LETTUCE, days_ago(20, 9, 20), growth(0.012, 4, (99, 1.10)), fixed(0.29, 0.58), disease="leaf_spot"),
    # 수확까지 끝난 재배 기록.
    Plant("끝난 재배 A", 5, LETTUCE, days_ago(62), growth(0.010, 4, (99, 1.09)), fixed(0.30, 0.58), ended=days_ago(32, 10)),
    Plant("끝난 재배 B", 6, LETTUCE, days_ago(58), growth(0.010, 4, (14, 1.12), (99, 1.06)), fixed(0.32, 0.56), ended=days_ago(28, 10)),
    Plant("끝난 재배 C", 4, LETTUCE, days_ago(45), growth(0.010, 4, (99, 1.10)), fixed(0.29, 0.60), ended=days_ago(12, 10)),
    Plant("끝난 재배 D(같은 날 다시 심음)", 5, LETTUCE, days_ago(26), growth(0.010, 4, (99, 1.10)), fixed(0.30, 0.59), ended=days_ago(2, 10)),
]


def detections(size, cx, cy, background, confidence, cut):
    aspect = rng.uniform(0.85, 1.2)
    w = math.sqrt(size * aspect)
    h = size / w
    cx += rng.uniform(-0.012, 0.012)
    cy += rng.uniform(-0.012, 0.012)
    if cut:
        cy = 1 - h / 2 + 0.002
    boxes = [{"cls": "Planting", "conf": round(confidence, 4), "x": round(cx, 5), "y": round(cy, 5), "w": round(w, 5), "h": round(h, 5)}]
    for _ in range(rng.choice((1, 2))):      # 식물 전체 박스 안의 잎 박스
        lw, lh = w * rng.uniform(0.3, 0.5), h * rng.uniform(0.3, 0.5)
        boxes.append({"cls": "Planting", "conf": round(max(0.3, confidence - rng.uniform(0.08, 0.25)), 4),
                      "x": round(cx + rng.uniform(-0.2, 0.2) * w, 5), "y": round(cy + rng.uniform(-0.2, 0.2) * h, 5),
                      "w": round(lw, 5), "h": round(lh, 5)})
    if background:
        boxes.append(dict(BACKGROUND, conf=round(rng.uniform(0.3, 0.45), 4)))
    return boxes


photo_rows = []
for plant in plants:
    end = min(plant.ended, now) if plant.ended else now
    day = plant.planted.replace(hour=0, minute=0)
    shot_index = 0
    while day <= end:
        for hour, minute in SLOTS:
            t = day + dt.timedelta(hours=hour, minutes=minute, seconds=rng.randint(0, 50))
            age = plant.age_days(t)
            if t < plant.planted or t >= end or age < plant.first_age:
                continue
            if plant.only_last_day and t.date() != now.date():
                continue
            shot_index += 1
            cx, cy, level = plant.centre_at(age)
            size = plant.size_at(age) * level * rng.uniform(0.96, 1.04)
            dark = plant.dark_dawn and hour == 5 and t.day % 2 == 0
            confidence = rng.uniform(0.40, 0.55) if dark else rng.uniform(0.86, 0.97)
            cut = int(age) in plant.cut_ages and hour != 5
            background = plant.background_every and shot_index % plant.background_every == 0
            boxes = detections(size, cx, cy, background, confidence, cut)
            largest = max(range(len(boxes)), key=lambda i: boxes[i]["w"] * boxes[i]["h"])
            big = boxes[largest]
            diseased = plant.disease and age >= 15
            photo_rows.append((t, t, round(max(b["conf"] for b in boxes), 4), 0.82 if diseased else 0.0,
                               plant.disease if diseased else "no_detection", "dummy.jpg", "/uploads/camera/dummy.jpg",
                               str(2 if age >= 14 else 1), plant.port, SERIAL,
                               round(big["w"] * 1280 * big["h"] * 1024, 2), round(big["w"] * big["h"], 5),
                               round(big["h"] * 1024, 2), round(big["w"] * 1280, 2), json.dumps(boxes, ensure_ascii=False), largest))
        day += dt.timedelta(days=1)

seed = [
    "-- 모니터링 화면 확인용 더미 데이터 (로컬 DB 전용). make_dummy_seed.py가 만든 파일이며 cleanup.sql로 되돌린다.",
    f"-- 생성 시각: {now}   기기: {SERIAL}   소유 계정 id: {owner}",
    "SET NAMES utf8mb4;",
    "START TRANSACTION;",
    f"INSERT INTO device (serial_number, created_at, updated_at, device_nickname, led_mode, led_on_time, led_off_time, led_status, photo_interval, port_status, status, user_id) "
    f"VALUES ({q(SERIAL)}, NOW(), NOW(), '더미 타워', 1, '06:00:00', '22:00:00', 1, 6, '11111111', 0, {owner});",
]
for plant in plants:
    seed.append(
        "INSERT INTO plant (created_at, updated_at, disease_result, germinated_at, matured_at, name, planted_at, port_index, "
        "device_serial, stage_index, species_id, active_port_index, end_reason, ended_at) VALUES "
        f"({q(plant.planted)}, NOW(), {q(plant.disease)}, NULL, NULL, {q(plant.name)}, {q(plant.planted)}, {plant.port}, {q(SERIAL)}, "
        f"{plant.stage()}, {plant.species}, {'NULL' if plant.ended else plant.port}, {q('HARVESTED') if plant.ended else 'NULL'}, {q(plant.ended)});")
columns = ("created_at, updated_at, confidence, disease_confidence, disease_result, file_name, file_path, growth_stage_result, "
           "port_index, device_serial, box_area, box_area_ratio, box_height, box_width, detections_json, selected_box_index")
for i in range(0, len(photo_rows), 100):
    chunk = photo_rows[i:i + 100]
    seed.append(f"INSERT INTO photo ({columns}) VALUES\n" + ",\n".join("(" + ", ".join(q(v) for v in row) + ")" for row in chunk) + ";")
seed.append("COMMIT;")
(out / "seed.sql").write_text("\n".join(seed) + "\n", encoding="utf-8")

cleanup = f"""-- 더미 데이터 되돌리기 (로컬 DB 전용). 더미 기기와 확인용 계정만 지운다.
SET NAMES utf8mb4;
START TRANSACTION;
DELETE FROM prediction WHERE plant_id IN (SELECT id FROM plant WHERE device_serial = '{SERIAL}');
DELETE FROM file WHERE diary_id IN (SELECT id FROM diary WHERE device_serial = '{SERIAL}');
DELETE FROM diary WHERE device_serial = '{SERIAL}';
DELETE FROM notice WHERE device_serial = '{SERIAL}';
DELETE FROM photo WHERE device_serial = '{SERIAL}';
DELETE FROM sensor_log WHERE serial_number = '{SERIAL}';
DELETE FROM plant WHERE device_serial = '{SERIAL}';
DELETE FROM device WHERE serial_number = '{SERIAL}';
DELETE FROM email_verification WHERE email = 'growlab-dummy@example.test';
DELETE FROM user WHERE username = 'growlab_dummy';
COMMIT;
"""
(out / "cleanup.sql").write_text(cleanup, encoding="utf-8")
print(f"plants {len(plants)}  photos {len(photo_rows)}  now {now}")
