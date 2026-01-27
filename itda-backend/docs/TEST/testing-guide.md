# 백엔드 테스트 가이드

이 문서는 이 레포에서 테스트 코드를 작성할 때 참고하는 간단한 가이드입니다.
짧고 실무적으로 유지합니다.

## 범위와 위치
- 테스트 소스는 `itda-backend/src/test/java/com/itda/backend` 아래에 둡니다.
- 메인 패키지 구조를 그대로 따라갑니다.
- 테스트 클래스는 `*Test` 접미사를 사용합니다.

## 테스트 타입(기본 권장)
- 단위 테스트 (가장 권장)
  - Mockito 사용: `@ExtendWith(MockitoExtension.class)`
  - 외부 의존성(Gemini/Veo/S3/DB Mapper)은 mock 처리
  - 불필요한 Spring 컨텍스트 로딩은 피합니다
- 슬라이스 테스트 (웹 레이어)
  - 컨트롤러는 `@WebMvcTest`를 기본으로 사용
  - 서비스/리포지토리/매퍼는 `@MockBean`
  - 보안 필터가 필요 없으면 `@AutoConfigureMockMvc(addFilters = false)`
- 통합 테스트 (전체 컨텍스트)
  - 진짜 Spring 컨텍스트가 필요할 때만 `@SpringBootTest` 사용
  - DB 연결이 필요하므로 CI 환경에서 실패할 수 있음

## 공통 패턴
- 파일 I/O 테스트는 `@TempDir` + `LocalFileStorage` 사용
- JSON 파싱은 컨텍스트 없이 `JobRequestParser` 단독 테스트
- `Job.builder()`로 테스트 픽스처 구성
- 검증 포인트는 아래에 집중
  - 입력 검증
  - 의존성 호출 여부
  - 반환값 정확성

## 워커 테스트 가이드
- `ImageGenerationWorker`
  - mock 대상: `GeminiImageClient`, `ImageStorage`, `AssetRegistrar`, `JobRequestParser`
  - `ImageStorageResult` 기준으로 `ExecutionResult` 검증
- `VideoGenerationWorker`
  - mock 대상: `VeoClient`, `LocalVideoStorage`, `AssetRegistrar`, `JobRequestParser`
  - `StoredAsset` 기준으로 `ExecutionResult` 검증
- `LocalImageStorage`
  - `@TempDir` 사용
  - contentType 기본값 처리 및 파일 생성 검증
- `S3ImageStorage`
  - `S3Client` mock + `S3StorageProperties.bucket` 세팅
  - contentType 정규화/확장자 규칙 검증
  - `PutObjectRequest` 캡처해서 인자 확인

## 컨트롤러 테스트(WebMvcTest)
- DB를 띄우지 않으려면 `@WebMvcTest(ControllerClass.class)`
- `MockMvc`로 엔드포인트 검증
- 예: `TestImageJobControllerTest`는 WebMvcTest로 전환해 DB 의존성 제거
- 컨트롤러가 `ObjectMapper`를 생성자 주입받는 경우
  - `@MockBean ObjectMapper`로 해결하거나
  - `@Import(JacksonAutoConfiguration.class)`로 자동 구성 추가

## CI(Jenkins) 주의사항
- Jenkins 기본 환경에는 MySQL이 없음
- `@SpringBootTest`가 DataSource를 띄우면
  `CannotGetJdbcConnectionException`으로 실패할 수 있음
- 기본은 단위/슬라이스 테스트로 구성하는 것을 권장

## 실행 명령어
- 전체 테스트:
  - `./gradlew test`
- 특정 테스트만:
  - `./gradlew test --tests "com.itda.backend.worker.video.VideoGenerationWorkerTest"`

## 자주 발생하는 오류
- `CannotGetJdbcConnectionException`
  - `@WebMvcTest`로 전환하거나 테스트 DB 구성
- `BeanCreationException`
  - 필요 이상으로 컨텍스트가 뜨는지 확인
- S3 테스트 실패
  - bucket 값이 세팅되어 있는지 확인
