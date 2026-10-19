# 상추 수확기 예측 수신

최신 RPi 패키지와 전체 적용 안내: `../../deployment/growlab_edge_20261018/README.md`.

RPi가 사진에서 만든 275차원 특징을 BE에 보내면 BE가 기존 EC2 `POST /predict`에서 수확일을 추론해 저장한다. BE는 영상 신경망을 다시 실행하지 않는다. `harvest_prediction`은 기존 다음 단계 예측 테이블과 분리되어 있으며 날짜 정답은 YOLO 수확기 확인이다.

필수 환경변수: `GROWLAB_EDGE_DEVICE_KEYS_JSON` (기기별 32자 이상 비밀키 JSON 객체). RPi와 동일한 키를 설정하고 HTTPS 엔드포인트를 사용한다. 새 형식의 사진 업로드는 키를 검증한다. 기존 사진 API의 구형 전송 호환성은 유지한다.

단, 키를 등록한 기기는 구형 전송으로 인증을 우회할 수 없다. 이 기기의 모든 사진 업로드에 키와 새 재배 식별정보가 필요하다. 키를 아직 등록하지 않은 기존 기기만 구형 전송을 유지한다.

Migration: `database/migrations/20261018_edge_harvest.sql`, 1회 적용. `photo`의 plant_id/captured_at/observation_id는 기존 행에서 NULL이다. 신규 예측은 plantId가 필수다. 구형 사진의 재배 식물 ID를 추측해서 채우지 않는다.

포트 전체 상태는 MQTT retained + HTTP GET `/api/device-edge/{serial}/ports`로 제공한다. `predictionCrop=lettuce`인 품종만 해당 모델을 실행한다. 초기 판별은 품종명에 상추/lettuce가 포함되는지로 동일하게 정의했다. 새로운 품종 이름/작물 추가 시 BE의 HarvestCrop과 FE의 isLettucePlant를 함께 변경한다.

기기 전송 상태: FEATURES_READY, HARVEST_READY, WAITING_ANCHOR, NO_DETECTION, UNSUPPORTED_CROP. BE가 EC2 응답을 받은 뒤 PREDICTED로 저장한다. 조회 전용 상태: WAITING_OBSERVATION, PORT_OFF. API는 임의의 정확도 퍼센트/신뢰구간을 만들지 않는다. 8시간 이상 관측 지연은 stale=true로 표시한다.

수확/고사 종료나 OFF 상태의 새 업로드는 409. 같은 plantId·관측 ID·촬영 시각의 재전송은 기존 사진을 반환한다. 최신 결과는 수신 순서가 아닌 촬영 시각 기준이다. 센서 기록은 어느 작물에서도 구형 단계 예측을 호출하지 않는다.
