# AWS 이메일 인증 및 게시글 조회수 반영

대상은 AWS 배포용 `GrowLab 2/GrowLab_BE`와 `GrowLab 2/Growlab_fe`입니다. 로컬 `project`의 `application.properties`, SMTP 계정 값, IntelliJ 실행 구성은 복사하지 않습니다.

## SMTP

Beanstalk 환경변수 `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`와 기존 SMTP 인증·TLS 설정을 사용합니다. 발신자는 기본적으로 `MAIL_USERNAME`이며 다른 검증된 발신자를 사용하려면 `MAIL_FROM`을 지정합니다. `SPRING_MAIL_USERNAME`으로 직접 주입하는 기존 구성도 발신자 기본값에 반영됩니다.

메일의 `From`을 명시하므로 서버 OS에서 로컬 이메일 주소를 알아내려다 발생하던 `can't determine local email address` 오류를 방지합니다. SMTP 예외는 HTTP 503, DB 등 다른 서버 예외는 HTTP 500을 반환하고 원인을 로그에 기록합니다. 비밀번호나 계정 값은 이 변경에 포함하지 않습니다.

## 조회수

로그인한 사용자는 `growLabPostView_u{사용자ID}` 쿠키로 중복 조회를 구분합니다. 같은 브라우저에서 계정을 바꿔도 다른 계정의 읽은 이력에 영향을 받지 않습니다. 비로그인 사용자는 기존 브라우저 쿠키를 사용합니다. 조회수 쿠키는 24시간 유지하며 새 게시글 조회를 기록할 때 만료 시간을 갱신합니다.

HTTPS 요청 또는 AWS 프록시의 `X-Forwarded-Proto: https` 요청에는 `Secure; HttpOnly; SameSite=None`을 설정합니다. HTTP 로컬 개발에서는 `HttpOnly; SameSite=Lax`를 사용합니다. 쿠키의 Domain은 강제로 지정하지 않습니다. AWS 프록시가 클라이언트 프로토콜을 정확하게 전달해야 합니다.

Amplify 프론트의 상세 요청은 현재 토큰을 요청 시점에 고정하고, 같은 게시글이어도 다른 토큰의 진행 중인 요청은 공유하지 않습니다. 기존 API 주소 환경변수와 `withCredentials: true` 설정을 유지합니다.

## 유지하는 배포 설정

- BE 기본 포트 `5000` 및 기존 환경변수 덮어쓰기
- RDS 접속, S3 저장소, MQTT, 추론 서버 설정
- Amplify `VITE_API_BASE_URL` 및 빌드 설정
- 기존 `CORS_ALLOWED_ORIGINS`와 자격증명 허용 설정

코드 반영 후 BE와 FE를 각각 기존 배포 절차로 배포해야 운영 서버에 적용됩니다. 실제 배포, 환경변수 변경, 메일 발송은 이번 로컬 반영 작업에 포함하지 않습니다.

참고: [브라우저 쿠키 속성](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie), [AWS 프록시 헤더](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/x-forwarded-headers.html).
