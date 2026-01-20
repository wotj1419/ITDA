# CI/CD 실행 계획 (Jenkins + GitLab + EC2)

## 목적
- Jenkins 기반 CI/CD를 표준화하고, 이후에도 동일한 절차로 운영할 수 있게 정리한다.

## 범위
- CI: 백엔드/프론트 빌드 중심
- CD: EC2 배포 (JAR + systemd 방식으로 확정)

## 현재 상태
- Jenkinsfile 기반 파이프라인 구성 완료(Checkout/BE Build/FE Install/FE Build)
- GitLab Webhook 연동 진행 및 트러블슈팅 정리
- 상세 설정 값은 민감 정보로 분리(`docs/infra/infraSetting.md`)

## 표준 파이프라인 흐름(권장)
1) Checkout
2) Backend Build
3) Frontend Install
4) Frontend Build
5) Deploy(브랜치 조건)
- Backend Test는 팀 합의 후 추가

## Jenkinsfile 관리 기준
- 위치: 레포 루트 `Jenkinsfile`
- 체크아웃: `checkout scm` 사용
- 기본 체크아웃 제거: `options { skipDefaultCheckout() }`
- `sh` 사용을 전제로 하므로 Linux 에이전트 권장

## Jenkins Job 설정 기준
- Definition: `Pipeline script from SCM`
- SCM: Git
- Repository URL: 프로젝트 GitLab URL
- Credentials: GitLab PAT/SSH
- Branch Specifier: 운영 브랜치 정책에 맞게 지정(`*/develop`, `*/main` 등)
- Script Path: `Jenkinsfile`
- Build Triggers: GitLab Push/MR 이벤트

## GitLab Webhook 구성
- Webhook URL은 Jenkins Job 설정에서 제공되는 값을 그대로 사용
- 토큰/URL/egress IP 등 민감 정보는 `docs/infra/infraSetting.md`에 기록
- 네트워크 이슈 발생 시 소스 IP 확인 후 보안그룹에서 제한 허용

## 배포 방식 확정: JAR + systemd

### 1) EC2 사전 준비
- 배포 디렉터리: `/opt/itda`
- 릴리즈 디렉터리: `/opt/itda/releases`
- 현재 버전 링크: `/opt/itda/current/app.jar`
- systemd 서비스 파일 준비(예: `itda-backend.service`)

예시 systemd 유닛:
```ini
[Unit]
Description=itda-backend
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/itda
ExecStart=/usr/bin/java -jar /opt/itda/current/app.jar
Restart=always
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

### 2) 배포 스크립트 예시 (Jenkins에서 실행)
> 로컬 빌드 산출물(JAR)을 EC2로 전송하고, symlink 교체 후 재시작한다.
```bash
#!/usr/bin/env bash
set -euo pipefail

HOST="$1"                 # 예: ubuntu@<EC2_HOST>
JAR_PATH="$2"             # 예: itda-backend/build/libs/app.jar
APP_DIR="/opt/itda"
RELEASE_DIR="${APP_DIR}/releases"
TS=$(date +%Y%m%d%H%M%S)
REMOTE_JAR="${RELEASE_DIR}/app-${TS}.jar"

ssh "$HOST" "mkdir -p ${RELEASE_DIR}"
scp "$JAR_PATH" "$HOST:$REMOTE_JAR"
ssh "$HOST" "ln -sfn $REMOTE_JAR ${APP_DIR}/current/app.jar"
ssh "$HOST" "sudo systemctl restart itda-backend"
ssh "$HOST" "sudo systemctl --no-pager status itda-backend"
```

### 3) Jenkins Deploy 스테이지 예시
```groovy
stage('Deploy') {
  when {
    branch 'develop'
  }
  steps {
    dir('itda-backend') {
      sh 'gradle clean assemble -x test'
    }
    sshagent(credentials: ['ec2_ssh_key']) {
      sh 'bash scripts/deploy.sh ubuntu@<EC2_HOST> itda-backend/build/libs/<APP>.jar'
    }
  }
}
```

## Jenkinsfile 예시 (요약)
```groovy
pipeline {
  agent any
  tools {
    gradle 'S14P_gradle'
    nodejs 'Node-22'
  }
  options {
    skipDefaultCheckout()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
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

## 운영 체크리스트
- Webhook 테스트 결과 200 확인
- Jenkins Build History에 자동 트리거 기록 확인
- 배포 후 헬스체크 성공 확인
- 실패 시 로그/재시도 절차 문서화

## 다음 작업
- Deploy 스테이지를 Jenkinsfile에 반영
- 배포 스크립트 경로/파일명 확정(`scripts/deploy.sh`)
- 테스트 단계 포함 여부 최종 결정