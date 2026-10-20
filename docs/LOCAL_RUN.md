# project/GrowLab_BE 로컬 실행

대상 프로젝트: `C:/Users/dhgus/Desktop/project/GrowLab_BE`

## IntelliJ

Gradle 프로젝트를 동기화한 뒤 `GrowLab Local` 실행 구성을 선택합니다. 이 구성은 API 포트를 `8080`으로 지정합니다. 프로젝트의 기존 Spring Boot 기본 포트도 `8080`입니다. 프론트는 `C:/Users/dhgus/Desktop/project/Growlab_fe`이며 기본 API 주소가 `http://localhost:8080`입니다.

프로젝트 SDK와 Gradle JVM은 Java 17을 선택합니다. 현재 터미널의 `JAVA_HOME`은 존재하지 않는 `C:/DEV/jdk11`을 가리킵니다. 터미널 실행 시에는 설치된 JDK를 지정합니다.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.15.6-hotspot'
.\gradlew.bat bootRun --args='--server.port=8080'
```

콘솔에서 `Tomcat started on port 8080`과 애플리케이션 시작 완료를 확인한 뒤 프론트를 새로고침합니다. 시작에 실패했다면 첫 번째 `Caused by`부터 확인합니다.

## 이메일 인증 오류

이 프로젝트의 `application.properties`에는 Gmail SMTP 호스트와 계정 설정이 있습니다. 설정이 존재한다는 사실만으로 실제 연결이나 인증 성공이 보장되지는 않습니다.

- SMTP 연결·인증·발송 예외는 HTTP 503으로 응답하고 IntelliJ 콘솔에 원인 예외를 기록합니다.
- DB 등 다른 서버 오류는 HTTP 500을 유지하며 원인 예외를 기록합니다.
- 이메일 중복 등 기존 입력 오류는 HTTP 400을 유지합니다.

`AuthenticationFailedException`이면 Gmail 계정과 앱 비밀번호 설정을 확인합니다. 접속 거부 또는 시간 초과라면 `spring.mail.host`, `spring.mail.port`, 네트워크 연결을 확인합니다. TLS 오류는 SMTP 공급자 설정과 `spring.mail.properties.mail.smtp.starttls.enable`을 확인합니다. 기존 계정 값은 이번 변경에서 수정하지 않았습니다.

`ERR_CONNECTION_REFUSED`는 해당 주소에서 API 서버에 연결할 수 없다는 뜻입니다. 백엔드 종료·시작 실패·포트 변경 여부를 확인합니다. 이 경우 Axios의 `Network Error`는 같은 연결 실패에 따른 오류입니다.
