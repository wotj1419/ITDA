# 인프라 작업 기록 (용호)
# Day1 - 인프라/CI-CD

## 목표
- 로컬 표준 Docker Compose(MySQL/Redis/S3 LocalStack) 실행 방식 확정
- Jenkins 파이프라인 표준화/안정화(빌드·테스트 stage, PR 체크) 방향 정리
- 초보자도 따라 할 수 있는 실행/검증 절차 문서화

## 오늘 할 일 (타임박스)
1) 현황 파악 (20~30분)
- 기존 Compose 파일/문서 확인
- Jenkins 현 구성(이미 진행된 상태) 확인

2) Docker Compose 로컬 표준 확정 (60~90분)
- 표준 파일명/명령 확정
- 환경변수/포트 정책 정리
- LocalStack 버킷 초기화 확인

3) Jenkins 파이프라인 표준화/안정화 (90~120분)
- 표준 stage 정의
- PR 체크 기준 확정
- 빌드/테스트 명령 확정

4) 검증 체크리스트 작성 (30~40분)
- 서비스 기동/헬스체크 확인
- CI 파이프라인 성공 기준 확인

## 상세 작업

### 1) 현황 파악
- Compose 파일 확인
  - `docker-compose.s3.yml` 기준으로 서비스/포트/헬스체크 확인
- 관련 문서 확인
  - `docs/ai-movie-studio-md-pack-v3/12-local-dev-runbook.md`
  - `docs/ai-movie-studio-md-pack-v3/13-runbook-devops.md`
- Jenkins
  - 기존 Job/Stage/트리거(파이프라인/PR 여부) 스냅샷 정리

### 2) Docker Compose 표준화
- 표준 파일명 결정
  - 옵션 A: `docker-compose.yml`로 통일
  - 옵션 B: `docker-compose.s3.yml` 유지 + 명령 고정
- `.env.example` 작성(기본값/포트 정책)
- 실행 방법 문서화
  - 상세 문서: `docs/infra/local_compose_standard.md`

```bash
# 예시
# docker compose -f docker-compose.s3.yml up -d
docker compose up -d
```

- LocalStack 초기화 확인
  - 버킷 생성/존재 확인 방법 문서화

#### 확정 사항(현재)
- 표준 파일명: `docker-compose.s3.yml` 유지
- 실행 명령: `docker compose -f docker-compose.s3.yml up -d`
- 환경변수 기준(기본값 포함)
  - `MYSQL_ROOT_PASSWORD` (default: `ssafy`)
  - `MYSQL_DATABASE` (default: `itda_local`)
  - `MYSQL_PORT` (default: `3307`)
  - `REDIS_PORT` (default: `6379`)
  - `LOCALSTACK_TAG` (default: `latest`)
  - `LOCALSTACK_PORT` (default: `4566`)
  - `AWS_DEFAULT_REGION` (default: `ap-northeast-2`)
  - `AWS_ACCESS_KEY_ID` (default: `test`)
  - `AWS_SECRET_ACCESS_KEY` (default: `test`)
  - `S3_BUCKET` (default: `itda-local`)
- LocalStack 초기화
  - `localstack-init` 서비스가 `S3_BUCKET`을 자동 생성
  - 확인 명령: `aws --endpoint-url=http://localhost:4566 s3 ls`

### 3) Jenkins 파이프라인 표준화/안정화
- 표준 stage 정의
  - Checkout → Init → Backend Build → Backend Test → Frontend Install → Frontend Build → Frontend Test → Deploy(브랜치 조건)
- 확정 기준(현재)
  - 표준 stage: Checkout → Backend Build → Frontend Install → Frontend Build
  - 테스트 정책: 테스트 코드 미작성 → test stage 제외, `-x test` 유지
  - PR 체크 기준: MR/Push 이벤트 시 Jenkins 빌드 자동 실행, Status Check 성공 시에만 머지
- PR 체크 정책
  - Pipeline에서 GitLab MR 이벤트/Push 이벤트 트리거
  - Jenkins 상태 체크(Status Check)를 MR에 표시
  - 강제 머지 차단은 GitLab 권한/정책에 따라 별도 설정 필요
- 빌드/테스트 명령 정리
  - BE: `./gradlew clean assemble`, `./gradlew test` (gradlew 없으면 `gradle` 사용)
  - FE: `npm ci`, `npm run build`, `npm test`(스크립트 없으면 스킵)
- Jenkinsfile 적용 절차
  - 레포 루트에 `Jenkinsfile` 추가/커밋
  - Jenkins에서 Pipeline Job 생성 후 레포 연결
  - GitLab 플러그인으로 MR/Push 이벤트 트리거 설정
  - Webhook 등록 → MR/Push 시 자동 빌드
- 배포 단계(초안)
  - `develop`/`develope` → dev, `main` → prod로 분기
  - 실제 배포 명령은 대상(EC2/Docker/SSH)에 맞춰 교체

### Jenkins Job 생성(스크립트 방식) 및 Pipeline 스크립트(테스트 제외)
#### 1) 사전 준비(도구 설치)
- Gradle: `Manage Jenkins → Global Tool Configuration → Gradle`에 `8.5` 등록
  - 이름 예시: `S14P_gradle_8_5` (프로젝트 `gradle-wrapper.properties` 기준)
- NodeJS: `Manage Jenkins → Global Tool Configuration → NodeJS`에 `20.19+` 또는 `22.12+` 등록
  - 이름 예시: `Node-22`

#### 2) Pipeline Job 생성
- `New Item → Pipeline`
- General: `GitLab connection` 선택
- Pipeline
  - Definition: `Pipeline script`
  - 아래 스크립트 붙여넣기
- Build Triggers
  - `Build when a change is pushed to GitLab`
  - (가능하면) `Trigger on Merge Request events`

#### 3) Pipeline script (테스트 제외)
```groovy
pipeline {
  agent any
  tools {
    gradle 'S14P_gradle_8_5'
    nodejs 'Node-22'
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'master',
            credentialsId: 'gitlab_PAT',
            url: 'https://lab.ssafy.com/s14-webmobile1-sub1/S14P11C205.git'
      }
    }

    stage('Backend Build') {
      steps {
        dir('itda-backend') {
          sh 'gradle clean assemble -x test'
        }
      }
    }

    stage('Frontend Install') {
      steps {
        dir('itda-frontend') {
          sh 'npm ci'
        }
      }
    }

    stage('Frontend Build') {
      steps {
        dir('itda-frontend') {
          sh 'npm run build'
        }
      }
    }
  }
}
```

### 4) 검증 체크리스트
- Compose
  - `docker compose ps`에서 MySQL/Redis/LocalStack 정상
  - LocalStack S3 버킷 존재 확인
- Jenkins
  - MR 1건 생성 후 Build/Test 통과
  - MR 화면에 Jenkins 상태 체크 표시
  - 실패 로그 위치/재실행 절차 확인

## 산출물
- 로컬 표준 Compose 실행 가이드 문서화
- Jenkins 파이프라인 표준 stage/PR 체크 기준 문서화
- 간단 검증 체크리스트

## 메모/리스크
- Jenkins 설정은 이미 진행 중인 상태라, 기존 구조 변경 전에 현재 설정을 꼭 캡처
- Compose 파일명/명령은 팀 합의 필요

## GitLab 기준: Pipeline + MR 체크 설정 절차

### 0) 준비물
- Jenkins 접속 URL(외부에서 접근 가능한 주소)
- GitLab 프로젝트 URL(`https://gitlab.example.com/group/project`)
- GitLab Personal Access Token(`api`, `read_repository`)

### 1) 플러그인 설치(최소)
- Pipeline
- Git
- GitLab
- Credentials Binding

### 2) GitLab 토큰/자격증명 준비
- GitLab에서 Personal Access Token 생성(`api`, `read_repository` 권한)
- Jenkins `Manage Jenkins → Credentials`에 토큰 저장

### 3) Jenkins 전역 설정
- `Manage Jenkins → Configure System → GitLab`
  - GitLab URL 등록
  - Credentials(토큰) 선택
  - Test Connection 성공 확인
- Jenkins Root URL 설정(웹훅에 필요)

### 4) Pipeline Job 생성(상세)
- `New Item → Pipeline` 생성
- General
  - GitLab connection 선택(전역에 등록한 GitLab)
- Pipeline
  - Definition: `Pipeline script from SCM`
  - SCM: Git
  - Repository URL: GitLab 프로젝트 URL
  - Credentials: Git 접근용 토큰/SSH 선택
  - Branch Specifier: `*/develop` 또는 `*/main` (팀 규칙)
  - Script Path: `Jenkinsfile`
- Build Triggers
  - `Build when a change is pushed to GitLab` 체크
  - Trigger on Merge Request events 체크(가능한 경우)
  - Secret token 생성/저장(웹훅에서 사용)

### 5) GitLab Webhook 등록(상세)
- Jenkins Job 설정 화면에서 제공되는 Webhook URL 복사
- GitLab `Settings → Webhooks`에 등록
  - URL: Jenkins에서 제공한 Webhook URL
  - Secret Token: Jenkins에 설정한 토큰
  - Events: Push, Merge request
  - `SSL verification`은 환경에 맞게 체크
- Webhook 테스트 전달 확인

### 6) MR 체크 정책
- Jenkins 상태 체크가 MR에 표시되는지 확인
- GitLab에서 외부 CI 강제 옵션이 있다면 활성화
- 기본 운영 규칙: “Jenkins 성공 확인 후 머지”

### 7) 검증 절차
- MR 1건 생성 → Jenkins 자동 빌드 확인
- GitLab MR 화면에 Jenkins 상태 체크 표시 확인
- 실패 시 로그/원인 확인 후 재실행

### 8) 실패 시 체크리스트
- Test Connection 302
  - GitLab URL이 http/https 또는 서브패스인지 재확인
- Webhook 미수신
  - Jenkins Root URL/방화벽/보안그룹 확인
  - GitLab Webhook 테스트 결과 확인
- MR 상태 미표시
  - GitLab 플러그인 설정 확인
  - Jenkins Job에서 GitLab connection 지정 여부 확인
  - 필요 시 Jenkinsfile에 GitLab 상태 업데이트 단계 추가

## 작업 요약
- Jenkins UI 파이프라인을 `Jenkinsfile`로 이관하고 `checkout scm`으로 체크아웃 방식 정리
- Jenkins Job을 `Pipeline script from SCM` 방식으로 전환하는 절차 정리
- GitLab Webhook 기반 자동 빌드 설정 문서화

## 진행 내역
- `Jenkinsfile` 작성 및 스테이지 구성(Checkout/BE Build/FE Install/FE Build)
- Jenkins Job 설정에서 SCM 기반 파이프라인 사용 안내 정리
- 자동 빌드 가이드 문서 추가: `docs/infra/autoBuild.md`
- Jenkins URL/웹훅 URL 정보는 민감 정보로 분리( `docs/infra/infraSetting.md` 참고)

## 트러블슈팅
- Webhook timeout 발생
  - 원인: Jenkins 외부 접근 IP/포트 문제
  - 조치: 보안그룹에서 8081 인바운드 허용 후 재시도
- 실제 Webhook egress IP 확인
  - tcpdump 결과 소스 IP는 민감 정보로 분리( 담당자 `infraSetting.md` 참고)
  - 보안그룹 소스 제한 필요(임시 전체 오픈 후 IP 확인)

## 다음 할 일
- CD 작업 시작
- 개인 EC2 -> 팀 EC2 
- Webhook 재테스트 및 자동 빌드 성공 확인 (완료)
- Docker Compose 로컬 표준 확정 (파일명/명령/환경변수/LocalStack 초기화)
- Jenkins 표준 stage 정의 확정(테스트/PR 체크 기준 포함)
- PR 체크 정책 확정(머지 차단 조건, 상태 체크 표시)
- 검증 체크리스트 작성/실행
