# CI/CD 실행 계획 (Jenkins + GitLab + EC2)

## 상태

- 과거 결정/폐기: 현재 배포 방식은 Docker Compose 기반으로 전환됨

## 목적

Jenkins 기반 CI/CD를 표준화하고, 이후에도 동일한 절차로 운영할 수 있게 정리한다.

## 범위

- CI: 백엔드/프론트 빌드 중심
- CD: EC2 배포

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

## 배포 방식 (폐기됨)

### JAR + systemd

- 현재는 사용하지 않음 (Compose 기반으로 전환)
- 참고용으로만 유지하며, 실제 배포는 `docs/infra/deployment-ops-plan.md`를 따른다
