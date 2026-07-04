# Performance Reports

프론트엔드 성능 측정, 기준선, 개선 계획, 개선 후 결과를 모아두는 폴더입니다.

## Folder Structure

```txt
performance-reports/
  README.md
  baselines/
    FRONTEND_PERFORMANCE_BASELINE.md
  guides/
    FRONTEND_NGINX_DEPLOY_PERFORMANCE_GUIDE.md
  measurements/
    before/
      lighthouse_desktop_before.json
      lighthouse_mobile_before.json
    after/
      phase1/
      phase2/
  plans/
    FRONTEND_PERFORMANCE_OPTIMIZATION_PLAN.md
```

## Contents

```txt
baselines/
  현재 Vercel 배포 기준 Lighthouse 결과와 성능이 잘 나온 코드 근거를 정리합니다.

guides/
  배포와 측정 방법을 정리합니다.

measurements/
  Lighthouse JSON 등 원본 측정 자료를 보관합니다.

plans/
  성능 개선 작업 순서와 검증 계획을 정리합니다.
```

## Naming Rule

측정 결과 파일은 아래 형식으로 저장합니다.

```txt
lighthouse_<device>_<timing>.json
```

예시:

```txt
lighthouse_desktop_before.json
lighthouse_mobile_before.json
lighthouse_desktop_after_phase1.json
lighthouse_mobile_after_phase1.json
```
