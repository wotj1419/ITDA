# Git Flow 가이드

## 브랜치 전략

### 브랜치 구조

```
master          ← 배포용 (안정 버전)
  └── develop   ← 개발 통합 브랜치
        └── feat/기능명   ← 기능 개발 브랜치
```

| 브랜치 | 용도 | 규칙 |
|--------|------|------|
| `master` | 배포 가능한 안정 버전 | 직접 push 금지, MR로만 병합 |
| `develop` | 개발 통합 브랜치 | 기능 완료 후 병합 |
| `feat/*` | 기능 개발 | develop에서 분기, 완료 후 삭제 |

### 브랜치 네이밍 컨벤션

```
feat/login
feat/video-generation
feat/project-management
```

---

## 개발 워크플로우

### 1. 기능 개발 시작

```bash
# develop 브랜치에서 최신 코드 가져오기
git checkout develop
git pull origin develop

# 새 기능 브랜치 생성
git checkout -b feat/feature-name
```

### 2. 기능 개발 중

```bash
# 작업 후 커밋
git add .
git commit -m "Feat : 기능 설명"

# 주기적으로 원격에 백업 (선택)
git push origin feat/feature-name
```

### 3. 기능 개발 완료

```bash
# develop 최신화
git pull origin develop

# 충돌 발생 시 로컬에서 해결 후
git add .
git commit -m "Fix : develop 병합 충돌 해결"

# feat 브랜치를 develop에 병합
git checkout develop
git merge feat/feature-name
git push origin develop

# feat 브랜치 삭제
git branch -d feat/feature-name
git push origin --delete feat/feature-name
```

### 4. Master 병합 (GitLab MR)

1. GitLab에서 **Merge Request** 생성
2. `develop` → `master` 방향으로 MR
3. 코드 리뷰 후 병합

---

## 커밋 컨벤션

### 커밋 원칙

> **하나의 커밋 = 하나의 기능/변경**
> 
> 여러 기능을 한 커밋에 묶지 말고, 기능 단위로 커밋을 나눠주세요.
> 이렇게 하면 코드 리뷰가 쉬워지고, 문제 발생 시 롤백도 간편합니다.

**❌ Bad:**
```bash
git commit -m "Feat : 로그인, 회원가입, 비밀번호 찾기 기능 추가"
```

**✅ Good:**
```bash
git commit -m "Feat : Add login feature"
git commit -m "Feat : Add signup feature"
git commit -m "Feat : Add password reset feature"
```

### 커밋 메시지 형식

```
[Type] : 커밋 제목

본문 (선택사항)
- 변경 이유나 상세 설명
- 관련 이슈 번호 등
```

### Commit Types

| Type | 설명 |
|------|------|
| **Feat** | 새로운 기능 추가 |
| **Fix** | 버그 수정 |
| **Env** | 개발 환경 관련 설정 |
| **Style** | 코드 스타일 수정 (세미 콜론, 인덴트 등) |
| **Refactor** | 코드 리팩토링 |
| **Design** | CSS 등 디자인 추가/수정 |
| **Comment** | 주석 추가/수정 |
| **Docs** | 내부 문서 추가/수정 |
| **Test** | 테스트 추가/수정 |
| **Chore** | 빌드 관련 코드 수정 |
| **Rename** | 파일 및 폴더명 수정 |
| **Remove** | 파일 삭제 |

### 커밋 메시지 예시

**제목만 작성하는 경우:**
```
Feat : Add user login feature
```

**본문도 작성하는 경우:**
```
Feat : Add user login feature

- JWT 기반 인증 방식 적용
- Access Token 만료 시간 1일 설정
- 로그인 실패 시 에러 메시지 반환
- Related to #12
```

**기타 예시:**
```
Fix : Fix validation error on signup
Design : Update main page layout
Docs : Update API documentation
```

---

## Merge Request 가이드

### MR 제목 형식

```
[Type] 기능/변경 요약
```

**예시:**
- `[Feat] 사용자 로그인 기능`
- `[Fix] 프로젝트 목록 로딩 오류 수정`
- `[Refactor] API 호출 로직 개선`

### MR 설명 템플릿

```markdown
## 변경 사항
- 주요 변경 내용 1
- 주요 변경 내용 2

## 테스트
- [ ] 로컬 테스트 완료
- [ ] 관련 기능 동작 확인

## 관련 이슈
- #이슈번호 (있는 경우)

## 스크린샷 (UI 변경 시)
```

---

## 충돌 해결 가이드

`git pull origin develop` 시 충돌이 발생하면:

1. 충돌 파일 확인: `git status`
2. 충돌 파일 열어서 수동 해결
   ```
   <<<<<<< HEAD
   내 코드
   =======
   develop의 코드
   >>>>>>> develop
   ```
3. 마커 삭제 후 원하는 코드로 수정
4. 해결 완료 후 커밋
   ```bash
   git add .
   git commit -m "Fix : develop 병합 충돌 해결"
   ```

---

## 요약 플로우차트

```
[develop에서 시작]
      │
      ▼
[feat/feature-name 브랜치 생성]
      │
      ▼
[기능 개발 & 커밋]
      │
      ▼
[git pull origin develop]
      │
      ▼
[충돌 있으면 해결]
      │
      ▼
[develop에 merge & push]
      │
      ▼
[GitLab MR: develop → master]
      │
      ▼
[리뷰 후 master 병합]
```
