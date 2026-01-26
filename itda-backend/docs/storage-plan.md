# Image Storage Plan (LocalStack -> S3)

## 목적
- 이미지 워커가 LOCAL/S3(LocalStack 포함) 저장을 설정으로 전환 가능하게 한다.
- 운영 전환 시 코드 변경 없이 설정만 교체할 수 있도록 한다.

## 범위
- Image 저장 경로 및 Asset 기록 방식
- 기존 Job 생성/실행 흐름은 변경하지 않음

## 현행 상태
- Image 워커가 로컬 파일 저장만 사용함.
- S3 저장 구현체는 존재하지만 실제 주입/사용되지 않음.
- Asset 저장 시 StorageProvider가 LOCAL로 고정됨.

## 목표 상태
- `storage.provider` 값에 따라 Local/S3 저장 구현체가 선택됨.
- 저장 결과의 `storageProvider`가 Asset에 기록됨.
- LocalStack 환경에서 S3 경로/버킷 동작 검증 가능.

## 구현 단계
1) 저장 추상화
   - Image 워커에서 `ImageStorage`를 주입받아 사용
   - Local 저장소는 `storage.provider=LOCAL`일 때 활성화
   - S3 저장소는 `storage.provider=S3`일 때 활성화

2) Asset 등록 개선
   - AssetRegistrar가 storage provider를 인자로 받아 기록

3) 환경별 설정 정리
   - Local: `storage.provider=LOCAL`
   - LocalStack: `storage.provider=S3` + endpoint/credentials/bucket/path-style
   - Real S3: endpoint 제거, 실제 키/버킷/리전 적용

## 환경 설정 예시
### LocalStack
```
STORAGE_PROVIDER=S3
S3_ENDPOINT=http://localhost:4566
S3_REGION=ap-northeast-2
S3_ACCESS_KEY=test
S3_SECRET_KEY=test
S3_BUCKET=itda-local
S3_PATH_STYLE=true
```

### Real S3
```
STORAGE_PROVIDER=S3
S3_REGION=ap-northeast-2
S3_ACCESS_KEY=...
S3_SECRET_KEY=...
S3_BUCKET=...
S3_PATH_STYLE=false
```

## 주의 사항
- `/api/nodes/{id}/content`는 로컬 파일 기준으로 동작함.
- S3 사용 시 결과 확인은 Job 응답의 presigned URL 사용을 권장.

## 테스트 체크리스트
- [ ] Local 환경에서 이미지 생성 후 `./uploads/ai/images/...` 저장 확인
- [ ] LocalStack 환경에서 S3 버킷에 이미지 업로드 확인
- [ ] Asset 테이블에 StorageProvider가 올바르게 기록되는지 확인

## 롤백
- `storage.provider=LOCAL`로 변경하면 로컬 저장 방식으로 즉시 복귀
