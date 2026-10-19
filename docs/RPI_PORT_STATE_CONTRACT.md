# 포트 재배 상태 동기화 — 서버 구현, RPi 수신 구현은 미적용

> 아래는 초기 작업 기록이다. 2026-10-18 후속 작업에서 `../../deployment/growlab_edge_20261018/`에 RPi 수신·OFF 스킵·재배 ID별 예측 및 업로드를 구현했다. 실제 기기에는 아직 배포하지 않았다. 포트 메시지에는 `predictionCrop`이 추가되었고 HTTP 상태 동기화도 지원한다. 최신 적용 안내는 해당 패키지 README.md를 따른다.

2026-10-18. 사용자의 요청으로 RPi/Arduino/ESP 코드는 변경하지 않았다. 3회 재촬영 로직도 변경하지 않았다. 서버 배포나 실제 MQTT 브로커 전송은 수행하지 않았다.

## 추가한 서버 동작

- 식물 등록, 수확/고사 종료, 등록 취소, 포트 ON/OFF, 재배 시작일 수정 시 기기의 전체 포트 상태를 발행한다.
- 기기 생성/등록 해제/삭제 및 서버 시작 시에도 상태를 발행한다. 기기 삭제 시에는 전체 OFF 스냅샷을 남긴다.
- 신규 토픽: `growlab/{serialNumber}/ports`, retained JSON. 기존 MQTT outbound 기본 QoS 1을 사용한다.
- DB 트랜잭션이 커밋된 후 포트 메시지를 보낸다. 롤백한 재배 상태를 RPi로 보내지 않는다.
- 기본 60초마다 등록된 기기의 DB 상태를 다시 발행하여 전송 누락/브로커 재시작에 대응한다. `mqtt.port-state-refresh-ms`로 간격을 바꿀 수 있다. 전송 시도/채널 접수는 RPi 수신 확인과 다르다.
- FE는 기존 '식물 등록 → 포트 ON' API 순서를 그대로 사용한다. 등록 직후에는 새 plantId/active=false, ON 완료 후 active=true가 전달된다.

## 메시지

```json
{
  "schemaVersion": 1,
  "serialNumber": "GROWLAB-G111",
  "generatedAt": "2026-10-18T03:00:00Z",
  "portStatus": "01000000",
  "ports": [
    {"portIndex": 0, "active": false, "plantId": null, "speciesId": null, "plantedAt": null},
    {"portIndex": 1, "active": true, "plantId": 123, "speciesId": 1, "plantedAt": "2026-10-18T12:00:00"}
  ]
}
```

실제 payload는 항상 0~7번 포트 8개를 포함한다. 예시는 뒤 6개를 생략했다. `active`는 포트 ON이며 종료되지 않은 식물 ID가 존재할 때만 true다. 종료된 식물은 제외한다. 빈 포트의 ON 비트만으로 촬영 대상으로 만들지 않는다.

`plantId`가 재배 구분 키다. 같은 포트에서 다음 식물을 키우면 새 ID가 된다. 등록했지만 아직 ON하지 않은 식물은 active=false이면서 plantId는 존재할 수 있다. `plantedAt`은 기존 DB의 KST 지역 시각 문자열이며 실제 파종일이라고 보장하지 않는다. `generatedAt`은 메시지 생성 UTC 시각이고 DB 단조 증가 revision은 아니다.

## RPi 구현 시 적용할 동작 (이번에는 구현하지 않음)

1. 신규 토픽을 구독하고 전체 8포트 스냅샷을 검증·저장한다. 최신 retained 값을 재연결 시 다시 수신한다. 브로커 ACL이 신규 토픽 구독을 허용하는지도 배포 때 확인한다.
2. 현재 plantId와 다른 새 plantId면 이전 식물의 단계/예측 이력을 이어 쓰지 않는다. 같은 plantId의 반복 메시지로 기록을 계속 초기화하지 않는다.
3. Arduino `[PHOTO] PORT:n` 수신 시 ACK를 보낸다. 해당 포트가 OFF라면 ESP에 CAPTURE를 보내지 않고 NEXT를 보내 다음 포트로 진행한다. 모터 경로는 기존 순서대로 유지한다.
4. ON 포트에서만 촬영·YOLO·업로드를 수행한다. 촬영 처리 중 OFF 또는 plantId 변경이 오면 이전 식물의 결과를 새 식물 관측으로 반영하지 않는다. 이를 위해 이후 사진 업로드 계약에도 촬영 당시 plantId/촬영 시각을 연결해야 한다.
5. 서버 재연결/수신 지연/중복 시 전체 상태를 다시 확인한다. 여러 서버의 동시 변경을 엄격히 정렬하려면 별도 영속 revision/수신 확인 계약이 필요하다. 현재 JSON 생성 시각만으로 분산 순서를 보장하지 않는다.

**현재 RPi는 이 토픽을 읽지 않으므로, 이번 서버 변경만으로 실제 OFF 포트 촬영이 멈추지는 않는다.**

## 센서 기준은 기존 공통 교집합 유지

`SharedEnvironment`는 재배 중인 모든 품종의 온도·습도·pH·TDS·조명 시간 범위를 교집합으로 계산한다. 혼합 품종 등록 시 필수 범위 누락 또는 교집합이 없으면 등록을 거부한다.

RPi/Arduino로 보내는 기존 `thresholds` CSV는 기기당 하나의 온도·습도·pH·TDS 범위다. 예를 들어 온도 18~26℃와 15~22℃는 18~22℃로 보낸다. 조명 시간 범위는 혼합 재배 검증에 쓰지만 이 8개 값 CSV에 포함되지 않으며 별도의 조명 스케줄 제어가 존재한다.

수확/고사로 식물을 종료하면 남은 식물들의 공통 범위를 재계산한다. 등록된 품종의 기준 자체가 수정되면 해당 품종을 재배하는 기기에 기존 AFTER_COMMIT 리스너가 범위를 다시 발행한다. 공유 공기·양액 센서이므로 포트마다 별도 측정값이나 별도 제어가 생기는 것은 아니다.

## 검증

`PortStateLifecycleTest`: 수확 OFF→새 ID 등록→ON, 고사 OFF→새 ID 등록→ON, 빈 ON 포트 제외, 커밋 이후 전송, 롤백 시 전송 안 함, DB 재동기화, 전송 실패 후 재시도.

기존 `MqttPublisherTest`, `PlantLifecycleTest`, `DeviceServiceCompatibilityTest`, `SpeciesThresholdMqttListenerTest`와 함께 로컬 테스트했다. 실제 하드웨어 동작 시험은 아니다.
