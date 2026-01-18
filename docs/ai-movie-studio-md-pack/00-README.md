# AI Movie Studio — 일정/아키텍처 패키지 (W3~W6)

이 폴더는 **팀원이 처음 읽어도 “전체 구조 + 내가 해야 할 일”이 바로 보이도록** 만든 문서 묶음입니다.

## 무엇부터 읽으면 되나
- 10분 내 전체 구조 파악: `02-Architecture-Overview.md`
- 이번 주 내가 할 일 체크: `01-Schedule-W3-W6.md`
- Worker/Queue 구현 시작: `03-Dispatcher-Queue-Worker-Guide.md`
- FE 실시간 반영 붙이기: `04-WebSocket-Event-Schema.md`
- 로컬 실행/검증: `05-Local-Dev-Runbook.md`

## 구성 파일
- `01-Schedule-W3-W6.md` : 3~6주차 일정표(역할/병렬화/의존성/체크리스트 포함)
- `02-Architecture-Overview.md` : 전체 아키텍처(텍스트 다이어그램) + 용어 정의 + 플로우 설명
- `03-Dispatcher-Queue-Worker-Guide.md` : Dispatcher/Queue/Worker 구현 규칙(Streams) + 샘플 코드
- `04-WebSocket-Event-Schema.md` : WebSocket 이벤트 스키마(서버→클라이언트) + 예시
- `05-Local-Dev-Runbook.md` : Docker Compose / 실행 순서 / 샘플 테스트(curl) / 트러블슈팅
- `06-Minimum-API-Contracts.md` : FE/BE 동시 개발용 최소 API 계약(요청/응답 예시)
- `07-Release-Demo-Checklist.md` : W6 통합/배포/데모 체크리스트

## 문서 사용 방법(팀 운영)
- W3 D1에 `06-Minimum-API-Contracts.md`, `04-WebSocket-Event-Schema.md`는 **팀 합의 후 잠금**(변경 시 PR+리뷰 필수)
- Worker 구현은 `03-Dispatcher-Queue-Worker-Guide.md` 규칙을 따라 **자율적으로 병렬 개발**
- E2E는 `01-Schedule-W3-W6.md`의 “W3 DoD 체크리스트”를 기준으로 합격/불합격을 판단
