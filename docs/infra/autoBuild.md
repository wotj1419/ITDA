# Jenkins 자동 빌드 설정 (GitLab Webhook)

> 목표: GitLab의 push/MR 이벤트로 Jenkins 빌드를 자동 실행한다.

---

## 1) 사전 요구사항
- Jenkins 플러그인: Git, Pipeline, GitLab, Credentials Binding
- GitLab Personal Access Token(`api`, `read_repository`)
- Jenkins Root URL 설정(Manage Jenkins → Configure System)

---

## 2) Jenkins Job 설정 (Pipeline script from SCM)

### 2.1 Pipeline
- Definition: `Pipeline script from SCM`
- SCM: Git
- Repository URL: `https://lab.ssafy.com/s14-webmobile1-sub1/S14P11C205.git`
- Credentials: `gitlab_PAT`
- Branch Specifier: `*/chore/jenkinsfile` (테스트) 또는 `*/master`
- Script Path: `Jenkinsfile`

### 2.2 Build Triggers(Jenkins > 해당 Job > 설정 > Trigger)
- `Build when a change is pushed to GitLab`
- `Trigger on Merge Request events` (옵션)

---

## 3) GitLab Webhook 등록
- GitLab 프로젝트 > Settings > Webhooks (Maintainer/Owner 권한 필요)
- URL: Jenkins Job 설정 화면의 Webhook URL
- Secret Token: Jenkins Job에 설정한 토큰
- Events: Push, Merge request
- SSL verification: 환경에 맞게 설정
- Webhook 테스트 전송으로 수신 여부 확인

---

## 4) 자동 빌드 확인
1) `chore/jenkinsfile` 브랜치에 push
2) Jenkins Build History에서 자동 트리거 확인
3) Console Log에서 checkout 브랜치 확인

---

## 5) 문제 해결 체크리스트
- Webhook 미수신: Jenkins Root URL/방화벽/보안그룹/GitLab Webhook 테스트 확인
- 403/인증 실패: Secret Token 불일치, PAT 권한 부족
- 잘못된 브랜치 빌드: Branch Specifier/`checkout scm` 사용 여부 확인

---

## 참고) Pipeline script 방식(UI 직접 입력)
- Definition: `Pipeline script`
- `git` 스텝에 URL/크리덴셜/브랜치 명시 필요
- 브랜치 변경 시 Jenkinsfile이 아니라 UI 스크립트를 수정해야 함