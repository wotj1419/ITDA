# 인프라 작업 기록 (용호)

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
- Webhook 재테스트 및 자동 빌드 성공 확인
