"""Loopback-only API fixture for the real GrowLab monitoring frontend.

No production database, AWS service, or real device is touched.
Run: python scripts/monitoring_demo_server.py
"""

from __future__ import annotations

import json
import time
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit


PORT = 18080
SERIAL = "GROWLAB-G111"
KST = timezone(timedelta(hours=9))


def now() -> datetime:
    return datetime.now(KST).replace(microsecond=0)


def local_date(days_ago: int) -> str:
    return (now() - timedelta(days=days_ago)).strftime("%Y-%m-%d %H:%M:%S")


def utc_time(delta_days: int = 0) -> str:
    return (now() + timedelta(days=delta_days)).astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def plant(plant_id: int, port: int, species: str, age: int, stage: int, disease: str | None = None) -> dict:
    return {
        "id": plant_id, "name": f"시연 {species} {port + 1}", "portIndex": port,
        "species": species, "speciesId": 1 if "상추" in species else 2,
        "stageIndex": stage, "stageName": ("파종기", "생육기", "수확기")[stage],
        "stageNames": ["파종기", "생육기", "수확기"],
        "stageDurationDays": [0, 7, 25], "stageCount": 3,
        "plantedAt": local_date(age), "germinatedAt": local_date(age - 7) if age > 7 else None,
        "maturedAt": None, "diseaseResult": disease,
        "minTemperature": 18, "maxTemperature": 26,
        "minHumidity": 50, "maxHumidity": 80,
        "minPh": 5.5, "maxPh": 6.5,
        "minTds": 600, "maxTds": 1200,
    }


PLANTS = [
    plant(101, 0, "청상추", 19, 1),
    plant(102, 1, "청상추", 19, 1),
    plant(103, 2, "적상추", 28, 2),
    plant(104, 3, "청상추", 2, 0),
    plant(105, 4, "청상추", 20, 1, "leaf_spot"),
    plant(106, 5, "딸기", 22, 1),
]


def device() -> dict:
    return {
        "serialNumber": SERIAL, "deviceNickname": "시연용 더미 타워", "status": True,
        "ledStatus": True, "photoInterval": 6, "ledMode": True,
        "ledOnTime": "06:00", "ledOffTime": "22:00", "portStatus": "11111100",
        "lastPhotoAt": local_date(0), "createdAt": local_date(30), "updatedAt": local_date(0),
        "plants": PLANTS,
    }


def harvest(plant_id: int) -> dict:
    common = {
        "plantId": plant_id, "capturedAt": utc_time(), "predictedAt": utc_time(),
        "modelVersion": "lettuce-ridge275-20261018-v1", "stale": False,
        "target": "YOLO_HARVEST_CONFIRMATION",
    }
    if plant_id == 103:
        return {**common, "status": "HARVEST_READY", "expectedAt": utc_time(), "remainingDays": 0,
                "anchor": "growing", "anchorAt": utc_time(-18)}
    if plant_id == 104:
        return {"plantId": plant_id, "status": "WAITING_OBSERVATION", "stale": False,
                "target": "YOLO_HARVEST_CONFIRMATION"}
    if plant_id not in {101, 102, 105}:
        return {"plantId": plant_id, "status": "UNSUPPORTED_CROP", "stale": False,
                "target": "YOLO_HARVEST_CONFIRMATION"}
    days = {101: 6, 102: 9, 105: 7}[plant_id]
    return {**common, "status": "PREDICTED", "expectedAt": utc_time(days),
            "remainingDays": days, "anchor": "growing", "anchorAt": utc_time(-12)}


def trend(port: int) -> dict:
    if port not in range(6):
        return {"portIndex": port, "points": [], "peerPoints": [], "ports": [],
                "sizeChange": {"ready": False}, "growthRate": {"status": "pending"},
                "reference": {"ready": False}, "latestRatio": None}
    if port == 3:
        return {"portIndex": port, "points": [], "peerPoints": [], "ports": [],
                "sizeChange": {"ready": False}, "growthRate": {"status": "pending"},
                "reference": {"ready": False}, "latestRatio": None}
    rates = {0: 6.2, 1: 1.4, 2: 4.1, 4: 3.8, 5: 5.2}
    verdicts = {0: "similar", 1: "slower", 2: "similar", 4: "similar"}
    base = {0: .027, 1: .021, 2: .062, 4: .032, 5: .040}[port]
    growth = 1 + rates[port] / 100
    points = []
    for i in range(7):
        day = now() - timedelta(days=6 - i)
        points.append({"date": day.strftime("%Y-%m-%d"), "elapsedDay": 13 + i,
                       "value": round(base * growth**i, 4)})
    ref_ready = port in verdicts
    return {
        "portIndex": port, "points": points, "peerPoints": [], "rank": None,
        "portCount": None, "gapPercent": None, "latestRatio": points[-1]["value"],
        "ports": [{"portIndex": p, "health": "warn" if p == 1 else "bad" if p == 4 else "good"}
                  for p in range(6)],
        "sizeChange": {"ready": True, "days": 6, "fromRatio": points[0]["value"],
                       "toRatio": points[-1]["value"],
                       "changePercent": round((points[-1]["value"] / points[0]["value"] - 1) * 100, 1)},
        "growthRate": {"status": "growing", "ratePerDay": rates[port],
                       "previousRatePerDay": rates[port] - .6, "observationDays": 7, "spanDays": 6},
        "reference": {"ready": ref_ready, "ratePerDay": 5.5 if ref_ready else None,
                      "lowRatePerDay": 4.4 if ref_ready else None,
                      "highRatePerDay": 6.8 if ref_ready else None,
                      "plantCount": 4 if ref_ready else None,
                      "verdict": verdicts.get(port)},
        "viewChangedDay": None,
    }


class DemoHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args: object) -> None:
        print("[demo]", fmt % args, flush=True)

    def send_json(self, value: object, status: int = 200) -> None:
        raw = json.dumps(value, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS")
        self.end_headers()
        self.wfile.write(raw)

    def do_OPTIONS(self) -> None:
        self.send_json({})

    def do_GET(self) -> None:
        parsed = urlsplit(self.path)
        path = parsed.path
        if path == "/api/devices":
            return self.send_json([device()])
        if path == "/api/notices":
            return self.send_json([{"id": 1, "deviceSerial": SERIAL, "noticeType": "시연용 알림",
                                    "message": "이 화면은 로컬 더미 데이터입니다.", "isRead": False}])
        if path == "/api/notices/unread/count":
            return self.send_json(1)
        if path.startswith("/api/sensor_logs/stream/"):
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            sensor = {"temperature": 22.4, "humidity": 63.0, "ph": 6.1,
                      "tds": 890, "water_level_status": True}
            try:
                self.wfile.write(b"event: connect\ndata: {}\n\n")
                while True:
                    payload = json.dumps(sensor, ensure_ascii=False).encode("utf-8")
                    self.wfile.write(b"event: sensor\ndata: " + payload + b"\n\n")
                    self.wfile.flush()
                    time.sleep(20)
            except (BrokenPipeError, ConnectionResetError):
                return
        if path.startswith("/api/sensor_logs/latest/"):
            return self.send_json({"temperature": 22.4, "humidity": 63.0, "ph": 6.1,
                                   "tds": 890, "water_level_status": True})
        if path.startswith("/api/harvest-predictions/"):
            try:
                plant_id = int(path.rsplit("/", 1)[1])
            except ValueError:
                return self.send_json({"error": "bad plant id"}, 400)
            return self.send_json(harvest(plant_id))
        if path == "/api/photos/growth-trend":
            try:
                port = int(parse_qs(parsed.query).get("portIndex", [0])[0])
            except ValueError:
                port = 0
            return self.send_json(trend(port))
        return self.send_json({"error": "Demo endpoint not found", "path": path}, 404)

    def do_POST(self) -> None:
        path = urlsplit(self.path).path
        if path == "/api/ai/advice":
            return self.send_json({"advice": "[환경 전반]\n현재 온도와 습도가 안정적인 범위입니다.\n[조명 관리]\n정기 촬영과 조명 일정을 유지해 주세요.\n[양액 시스템]\npH와 TDS를 주기적으로 확인해 주세요."})
        if path == "/api/ai/port-diagnosis":
            return self.send_json({"advice": "시연용 분석입니다. 최근 사진의 성장 속도와 환경 기록을 함께 확인해 주세요."})
        return self.send_json({"error": "Demo endpoint not found", "path": path}, 404)

    def do_PATCH(self) -> None:
        return self.send_json(device())


if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", PORT), DemoHandler)
    print(f"Monitoring demo API: http://127.0.0.1:{PORT}", flush=True)
    server.serve_forever()
