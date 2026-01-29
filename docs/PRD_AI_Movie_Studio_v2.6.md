# 🎬 AI Movie Studio - Product Requirements Document (PRD)

> **버전**: 2.6.0  
> **최종 수정일**: 2026-01-29  
> **프로젝트 기간**: 6주  
> **문서 목적**: 코드베이스 분석 기반 현행 구현 반영

---

## 문서 이력 (최신)

- v2.6.0 (2026-01-29): **코드베이스 분석 기반 현행 구현 반영**
  - 프론트엔드 코드베이스 분석을 통해 실제 구현된 기능 확인 및 반영
  - 구현 완료된 기능에 ✅ 표시, 진행 중인 기능에 🔄 표시
  - 주요 변경 사항:
    - **인증**: AuthPage 로그인/회원가입 UI 구현 완료 ✅
    - **대시보드**: DashboardPage 프로젝트 목록/생성 UI 구현 완료 ✅
    - **프로젝트 상세**: ProjectDetailPage 탭 구조(Story/Scenes/Objects) 구현 완료 ✅
    - **AI 시나리오 생성**: 4단계 플로우(프롬프트→줄거리→씬) scenario store 완전 구현 ✅
    - **노드 기반 씬 편집**: SceneEditPage + sceneNode store + NodeCanvas + 5종 노드 패널 완전 구현 ✅
    - **타임라인**: TimelinePage + timeline store + 머지/다운로드/순차재생 구현 완료 ✅
    - **협업**: collab store + WebRTC P2P + 화상통화/채팅/커서/프레즌스 구현 완료 ✅
    - **토스트 알림**: ui store + ToastContainer 구현 완료 ✅
    - **오브젝트 시트**: objects API + 캐릭터/소품 관리 구현 완료 ✅
  - 기존 v2.5.3 내용은 하단에 유지

---

## 목차

- 1. 개요 (Overview)
- 2. 문제 정의 (Problem Statement)
- 3. 타겟 사용자 (Target Users)
- 4. 핵심 기능 (Core Features)
- 5. 사용자 플로우 (User Flow)
- 6. 화면 구성
- 7. 기술 요구사항
- 8. API 명세 (초안)
- 9. 개발 일정 (6주)
- 10. 리스크 및 완화 방안
- 11. 성공 지표
- 12. 부록
- 13. 문서 이력

## 1. 개요 (Overview)

### 1.1 프로젝트 소개
**AI Movie Studio**는 AI 영화 제작자들이 **기획부터 완성까지 전체 워크플로우**를 하나의 플랫폼에서 진행할 수 있는 AI 영화 제작 스튜디오입니다.

> 한 줄 요약: **"아이디어만 있으면, AI가 당신의 영화를 만들어 드립니다"**

사용자는 시나리오 구상 → 스토리보드 이미지 생성 → AI 영상 변환 → 음악 추가 → 영상 편집의 전 과정을 AI 도구들과 함께 진행하며, WebRTC 기반 협업 기능으로 팀원들과 실시간으로 아이디어를 나눌 수 있습니다.

### 1.2 핵심 가치 제안 (Value Proposition)

| 가치 | 설명 |
|------|------|
| 🎭 **올인원 워크플로우** | 기획-스토리보드-영상생성-편집까지 하나의 플랫폼에서 |
| 🤖 **AI 파워 통합** | Gemini(이미지), Google Veo 3.1(영상) 등 Google Cloud AI 통합 |
| 👥 **실시간 협업** | WebRTC 화상통화로 팀원과 아이디어 공유 및 브레인스토밍 |
| 🎬 **프로젝트 관리** | 씬 단위로 체계적인 영화 제작 프로세스 관리 |
| ⚡ **빠른 프로토타이핑** | 아이디어를 1분 내외의 AI 영화로 빠르게 구현 |

### 1.3 핵심 워크플로우

```
┌─────────────────────────────────────────────────────────────────┐
│ 1️⃣ 기획/시나리오 설계                                            │
│    • WebRTC 화상통화로 팀원과 브레인스토밍                        │
│    • AI 시나리오 생성 도구로 스토리 아이디어 구체화                │
│    • 프로젝트 생성 및 씬 구조 설계                                │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2️⃣ 스토리보드 생성                                               │
│    • Gemini API로 각 씬의 스토리보드 이미지 생성                  │
│    • 마스터 샷 → 다양한 앵글(클로즈업, 오버더숄더 등) 생성        │
│    • 필요시 프롬프트 수정으로 이미지 정교화                       │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3️⃣ AI 영상 생성                                                  │
│    • 스토리보드 이미지 → AI 영상 변환 (Image-to-Video)            │
│    • Google Veo 3.1 API (P0), 오픈소스 모델 선택 옵션 (P1)        │
│    • 카메라 움직임, 분위기 등 프롬프트로 조절                     │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4️⃣ 음악 파일 업로드/보관                                         │
│    • 외부에서 생성한 배경음악 파일 업로드 (P1, 저장용)            │
│    • 편집/믹싱은 P2에서 검토                                      │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5️⃣ 영상 편집/조합                                                │
│    • 씬별 영상 클립들을 순서대로 병합                             │
│    • 필요시 분할/트리밍으로 구간 조절                             │
│    • 자막 추가 (선택)                                             │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 🎬 완성된 AI 영화                                                 │
│    • 미리보기 및 다운로드                                         │
└─────────────────────────────────────────────────────────────────┘
```
> 범위 메모: **P0는 배경음악/효과음 없이** 씬별 확정 영상 병합까지를 목표로 하고, **P1은 음악 업로드/보관**, **P2에서 믹싱/병합**을 검토합니다.


### 1.4 기술 스택

| 영역 | 기술 |
|------|------|
| Frontend | Vue.js 3 (Composition API), Vue Flow (노드 기반 에디터), Pinia (상태관리) |
| Backend | Spring Boot 3.x |
| 실시간 통신 | W3: STOMP 프로젝트 이벤트 WS(Job 완료/실패) / W5: Raw WS 기반 WebRTC·Chat·Presence |
| 실시간 동기화 | Yjs / CRDT (P1, 피그마 스타일 협업) |
| AI 이미지 생성 | Google Gemini API (Gemini 2.5 Flash Image, 기본값/환경설정 변경 가능) |
| AI 영상 생성 | Google Veo 3.1 API (P0), 오픈소스 모델 (P1: Stable Video Diffusion 등) |
| 음악/효과음 | 외부 파일 업로드/보관 (편집/믹싱은 향후 검토) |
| 영상 편집/병합 | Server-side FFmpeg (Worker 비동기 Job) |
| 데이터베이스 | MySQL (MVP 기준) |
| 파일 저장소 | 로컬 파일 저장(`uploads`) → S3/MinIO 전환 예정 |
| 메시지 큐 | Redis Streams (비동기 작업 처리) |
| 캐싱 | Redis (세션, API 응답 캐싱) |
| 웹서버/리버스 프록시 | Nginx |
| CI/CD | Jenkins |
| 모니터링/로깅 | Prometheus + Grafana (또는 CloudWatch) |
| API 문서화 | Swagger / OpenAPI |
| 인프라 | AWS EC2 / Docker |
| 컨테이너 레지스트리 | Docker Hub (또는 AWS ECR) |

---

## 2. 문제 정의 (Problem Statement)

### 2.1 현재 상황
- AI 영상 생성 도구(Runway, Luma 등)는 강력하지만, **각각 별도로 사용**해야 함
- 시나리오 → 이미지 → 영상 → 편집까지 **여러 도구를 오가며 작업**해야 하는 번거로움
- 팀 협업 시 아이디어 공유와 피드백이 분산되어 **커뮤니케이션 비용** 발생
- 기존 영상 편집 도구는 전문적이지만 **학습 곡선이 높음**

### 2.2 해결 목표
- **올인원 플랫폼**: 기획부터 완성까지 하나의 서비스에서 진행
- **AI 통합**: Google Cloud AI (Gemini, Veo 3.1) + 오픈소스 모델을 한 곳에서 활용
- **협업 지원**: WebRTC 화상통화로 실시간 브레인스토밍
- **간편한 편집**: 전문 지식 없이도 클립 병합/조합 가능

---

## 3. 타겟 사용자 (Target Users)

### 3.1 주요 페르소나

#### 페르소나 1: AI 영화 제작자 (Creator)
- **특징**: AI 도구에 관심이 많은 콘텐츠 크리에이터
- **니즈**: 아이디어를 빠르게 영상으로 구현하고 싶음
- **Pain Point**: 여러 AI 도구를 오가며 작업하는 것이 번거로움

#### 페르소나 2: 인디 영화 감독 (Director)
- **특징**: 저예산으로 창작 활동을 하는 영화 감독
- **니즈**: 비용 없이 영화 컨셉/프로토타입을 제작하고 싶음
- **Pain Point**: 실사 촬영 비용과 인력 확보가 어려움

#### 페르소나 3: 콘텐츠 팀 (Team)
- **특징**: 2-3명이 함께 영상 콘텐츠를 제작하는 팀
- **니즈**: 원격으로 아이디어를 공유하고 함께 작업하고 싶음
- **Pain Point**: 기획 단계에서 의견 조율과 피드백이 어려움

---

## 4. 핵심 기능 (Core Features)

### 4.1 기능 우선순위 및 구현 현황

> **범례**: ✅ 구현 완료 | 🔄 구현 중 | ⬜ 미구현

| 우선순위 | 기능 | 설명 | MVP | 구현 현황 |
|----------|------|------|-----|-----------|
| **P0** | 사용자 인증 | 회원가입, 로그인 (이메일/비밀번호) | ✅ | ✅ `AuthPage.vue`, `auth.ts` |
| **P1** | OAuth 소셜 로그인 | Google 등 소셜 로그인 지원 | ⬜ | ⬜ |
| **P0** | 프로젝트 관리 | 프로젝트 생성, 씬 관리 | ✅ | ✅ `DashboardPage.vue`, `ProjectDetailPage.vue`, `project.ts` |
| **P0** | 오브젝트 시트 생성 | 주요 오브젝트(캐릭터, 소품 등) 레퍼런스 이미지 생성 | ✅ | ✅ `objects.ts` API, `character.ts` store |
| **P0** | AI 시나리오 생성 | 키워드 → 시나리오 아이디어 제안 | ✅ | ✅ `scenario.ts` store (4단계 플로우) |
| **P0** | 협업 (플로팅 바) | 화상통화 + 화면공유, 화면 이동해도 통화 유지 | ✅ | ✅ `collab.ts` store, `CollabPanel.vue`, `CollabPill.vue` |
| **P0** | Presence 표시 | 팀원이 어느 화면에서 작업 중인지 실시간 표시 | ✅ | ✅ `PresencePanel.vue`, `CursorOverlay.vue` |
| **P0** | 노드 기반 씬 편집 | Vue Flow 기반 노드 캔버스로 마스터→샷→영상 흐름 시각화 | ✅ | ✅ `SceneEditPage.vue`, `sceneNode/index.ts`, `NodeCanvas.vue` |
| **P0** | 마스터 이미지 생성 | 씬의 기준점이 되는 와이드 샷 이미지 생성 + AI 프롬프트 개선 | ✅ | ✅ `MasterImagePanel.vue`, `addMasterImageNode()` |
| **P0** | 스토리보드 그리드 생성 | 마스터 기반 다양한 샷(OTS, 클로즈업 등)을 그리드 이미지로 생성 | ✅ | ✅ `StoryboardGridPanel.vue`, `addStoryboardGridNode()` |
| **P0** | 샷 추출 (AI 재생성) | 그리드에서 선택한 샷을 AI로 고품질 재생성 | ✅ | ✅ `ShotPanel.vue`, `addShotNode()` |
| **P0** | AI 영상 생성 (Veo 3.1) | Image-to-Video + 카메라 움직임 예시 선택 | ✅ | ✅ `VideoPanel.vue`, `addVideoNode()` |
| **P1** | AI 영상 생성 (오픈소스) | Stable Video Diffusion 등 오픈소스 모델 선택 옵션 | ⬜ | ⬜ |
| **P0** | 영상 병합 | 씬별 영상을 하나로 조합 (확정된 영상 기준) | ✅ | ✅ `TimelinePage.vue`, `timeline.ts` store |
| **P0** | 순차 재생 | 씬 내 클립 / 프로젝트 전체 영상 순차 자동 재생 | ✅ | ✅ `TimelinePlaybackModal.vue` |
| **P0** | 진행률 표시 | AI 생성 **상태(대기/진행중/완료/실패)** + 완료/실패 알림 표시 | ✅ | ✅ `ui.ts` store, `ToastContainer.vue`, WebSocket 이벤트 연동 |
| **P1** | 외부 파일 업로드 | 이미지/영상/음악 직접 업로드 | ⬜ | 🔄 `files.ts` API 구현됨 |
| **P1** | 퀵 프리뷰 | 프로젝트 상세에서 씬 연결 미리보기 | ⬜ | ⬜ |
| **P1** | 실시간 알림 | 협업 중 팀원 편집 알림 | ⬜ | 🔄 WebSocket 기반 구현 중 |
| **P1** | 영상 분할/트리밍 | 원하는 구간만 잘라내기 | ⬜ | ⬜ |
| **P1 🔥** | 실시간 동시 편집 | Yjs/CRDT 기반 피그마 스타일 실시간 협업 | ⬜ | ⬜ |
| **P2** | AI 음악 생성 | AI 음악 API 연동 (Suno 등, API 가용성 확인 필요) | ⬜ | ⬜ |
| **P2** | 자막 추가 | 영상에 텍스트 오버레이 | ⬜ | ⬜ |

### 4.2 상세 기능 명세 (구현 현황 반영)

#### 4.2.1 사용자 인증 ✅
```
구현 완료:
- 회원가입 (이메일/비밀번호) ✅
- 로그인/로그아웃 ✅
- 프로필 관리 (이름, 프로필 이미지) ✅
- 비밀번호 재설정 (이메일 링크/코드 기반) ✅
- Access Token + Refresh Token 기반 인증 ✅
- 한/영 다국어 지원 (AuthPage) ✅

파일 위치:
- src/pages/AuthPage.vue
- src/stores/auth.ts
- src/services/api/auth.ts
```

#### 4.2.2 프로젝트 관리 ✅
```
프로젝트 구조:
📁 프로젝트 (AI 영화 1편)
   ├── 📝 시나리오 (전체 스토리 개요)
   ├── 🎨 오브젝트 시트 (주요 오브젝트 레퍼런스 이미지: 캐릭터, 소품 등)
   └── 🎬 씬 목록
        ├── 씬 1
        │     ├── 설명 (씬 내용)
        │     ├── 등장 오브젝트 (오브젝트 시트에서 선택)
        │     ├── 스토리보드 이미지들
        │     ├── AI 생성 영상
        │     └── 음악/SFX
        ├── 씬 2
        └── ...

구현 완료:
- 프로젝트 생성 (제목, 설명, 장르) ✅
- 프로젝트 목록 조회 ✅ - DashboardPage.vue
- 씬 추가/수정/삭제/순서변경 ✅
- 팀원 초대 (가입된 유저를 **이메일**로 추가) ✅
- 프로젝트 탈퇴 (본인, Owner 제외) ✅
- 프로젝트 삭제 ✅
- 즐겨찾기 기능 ✅ - FavoritesPage.vue
- 휴지통 기능 ✅ - TrashPage.vue

파일 위치:
- src/pages/DashboardPage.vue
- src/pages/ProjectDetailPage.vue
- src/stores/project.ts
- src/services/api/projects.ts
```

##### 프로젝트 상세 화면 탭 구조 ✅

| 탭 | 역할 | 주요 기능 | 구현 |
|---|---|---|---|
| **Story** | 시나리오/스토리 관리 | AI 시나리오 생성, 씬 목록/순서 관리, 씬 설명 편집 | ✅ |
| **Scenes** | 영상 진행 현황 | 씬별 확정 영상 현황, 프로젝트 미리보기, 씬 편집 진입점 | ✅ |
| **Objects** | 오브젝트 시트 관리 | 캐릭터/소품 레퍼런스 이미지 생성 | ✅ |

#### 4.2.3 오브젝트 시트 생성 ✅
```
목적: 영화 전체에서 오브젝트(캐릭터, 소품 등) 외형의 일관성을 유지하기 위한 레퍼런스 이미지 생성

구현 완료:
- 오브젝트 생성/조회/수정/삭제 ✅
- 캐릭터 유형 지원 ✅
- AI 이미지 생성 Job 연동 ✅

파일 위치:
- src/stores/character.ts
- src/services/api/objects.ts
```

#### 4.2.4 AI 시나리오 생성 (4단계 플로우) ✅
```
구현 완료 - 4단계 플로우:

STEP 1️⃣ 기본 정보 입력 ✅
- 장르, 분위기/톤, 씬 개수 입력
- 고급 옵션: 주제/키워드, 메인 캐릭터 힌트, 배경 힌트, 참고 스타일

STEP 2️⃣ AI 시나리오 프롬프트 생성 ✅
- generatePrompt() 함수
- 프롬프트 검토/수정/재생성 지원
- 승인 기능 (approvePrompt)

STEP 3️⃣ 전체 줄거리 생성 ✅
- generatePlot() 함수
- 줄거리 검토/수정/재생성 지원
- 승인 기능 (approvePlot)

STEP 4️⃣ 씬별 스토리 생성 ✅
- generateScenes() 함수
- 씬 순서 변경 (reorderScenes)
- 개별 씬 편집/재생성/추가/삭제

파일 위치:
- src/stores/scenario.ts
- src/services/api/scenario.ts
- src/components/scenario/ (6개 컴포넌트)
```

#### 4.2.5 협업 (WebRTC) - 플로팅 협업 바 ✅
```
구현 완료:

핵심 기능:
- 화상 통화 시작/종료 ✅ - enableMedia(), disableMedia()
- 마이크 ON/OFF ✅ - toggleMute()
- 카메라 ON/OFF ✅ - toggleVideo()
- 화면 공유 ✅ - toggleScreenShare()
- 채팅 (sendMessage()) ✅
- Presence 표시 (updateLocation, updateCursor) ✅
- 커서 오버레이 ✅ - CursorOverlay.vue

WebRTC 구현:
- Mesh P2P 아키텍처 ✅
- WebSocket 시그널링 (socket.ts) ✅
- Offer/Answer/ICE Candidate 처리 ✅
- Speaking 모니터 (음성 활성화 감지) ✅

파일 위치:
- src/stores/collab.ts (646줄, 완전 구현)
- src/components/collab/CollabContainer.vue
- src/components/collab/CollabPanel.vue
- src/components/collab/CollabPill.vue
- src/components/collab/CursorOverlay.vue
- src/components/collab/PresencePanel.vue
- src/components/collab/ParticipantAvatar.vue
- src/services/ws/socket.ts
```

#### 4.2.6 스토리보드 생성 (노드 기반 워크플로우) ✅

**노드 타입 5가지 - 모두 구현 완료:**
| 타입 | 아이콘 | 출력 | 구현 |
|------|-------|------|------|
| 씬 헤더 | 📖 | - (정보 표시) | ✅ `ensureSceneHeaderNode()` |
| 마스터 이미지 | 🎬 | 1장 와이드샷 | ✅ `addMasterImageNode()` |
| 스토리보드 그리드 | 📐 | 1장 그리드 이미지 | ✅ `addStoryboardGridNode()` |
| 샷 | 📷 | 1장 고품질 이미지 | ✅ `addShotNode()` |
| 영상 | 🎥 | 영상 클립 | ✅ `addVideoNode()` |

**sceneNode Store 주요 기능 (1179줄):**
```typescript
// 파일: src/stores/sceneNode/index.ts

// State
nodes, edges, selectedNodeId, selectionMode, positionHistory

// Getters
selectedNode, activeMaster, confirmedVideos, childNodes

// Actions - Load
loadSceneNodes(), ensureSceneHeaderNode(), ensureActiveMasterNode()
hydrateNodeMedia(), hydrateVideoDurations()

// Actions - Add Nodes
addMasterImageNode(), addStoryboardGridNode()
addShotNode(), addVideoNode()

// Actions - Update
updateNodePosition(), updateNodeData()
generateForNode(), confirmNode(), unconfirmNode()

// Actions - Delete
deleteNode()

// WebSocket 이벤트 처리
handleProjectEvent() - Job 완료/실패 시 노드 상태 자동 업데이트
```

**Vue Flow 캔버스:**
```
파일 위치:
- src/pages/SceneEditPage.vue
- src/components/scene-editor/NodeCanvas.vue (8KB)
- src/components/scene-editor/NodeDeleteConfirmModal.vue

노드 컴포넌트들:
- src/components/scene-editor/nodes/ (6개)

패널 컴포넌트들:
- src/components/scene-editor/panels/ (8개)
  - MasterImagePanel.vue
  - StoryboardGridPanel.vue
  - ShotPanel.vue
  - VideoPanel.vue
  - SceneHeaderPanel.vue
  - ...
```

#### 4.2.7 AI 영상 생성 (Image-to-Video) ✅
```
구현 완료:
- 시작 샷 이미지 필수 ✅
- Provider: Google Veo 3.1 ✅
- 카메라 움직임 옵션 (줌인, 줌아웃, 패닝, 틸트, 정적) ✅
- 모션 설명 필드 ✅
- 비동기 Job 생성 → 상태 추적 ✅
- 진행률 표시 (status 기반) ✅
- 백그라운드 생성 지원 ✅
- "타임라인에 확정" 버튼 ✅

파일 위치:
- src/components/scene-editor/panels/VideoPanel.vue
- src/services/api/ai.ts (6KB)
```

#### 4.2.8 영상 편집 (2단계 병합 구조) ✅
```
타임라인 Store 주요 기능 (362줄):
- loadClips() - 프로젝트/씬별 클립 로드 ✅
- reorderClips() - 클립 순서 변경 ✅
- removeClip() - 클립 삭제 (확정 취소) ✅
- startMerge() - 병합 시작 ✅
- resetMerge() - 병합 초기화 ✅

Getters:
- orderedClips - 정렬된 클립 목록 ✅
- totalDuration - 총 재생 시간 ✅
- clipCount - 클립 개수 ✅
- canMerge - 병합 가능 여부 ✅
- canDownload - 다운로드 가능 여부 ✅
- mergeStatus - 병합 상태 (idle/merging/done/error) ✅

컴포넌트:
- TimelinePage.vue (9.5KB) ✅
- ClipItem.vue ✅
- VideoTrack.vue ✅
- TimeRuler.vue ✅
- MergeProgress.vue ✅
- TimelinePlaybackModal.vue - 순차 재생 모달 ✅
- VideoPreview.vue ✅

WebSocket 이벤트 연동:
- handleProjectEvent() - 병합 완료/실패 이벤트 처리 ✅
- 폴링 Fallback 지원 ✅
```

#### 4.2.9 토스트 알림 시스템 ✅
```
구현 완료:
- showToast(), removeToast() 함수 ✅
- 타입별 스타일 (success, error, warning, progress) ✅
- 자동 타임아웃 ✅
- Job 완료/실패 시 알림 표시 ✅

파일 위치:
- src/stores/ui.ts
- src/components/common/ToastContainer.vue
- src/composables/useGenerationToast.ts
```

### 4.3 UX 가이드라인 (구현 반영)

#### 4.3.1 노드 기반 워크플로우 원칙 ✅
```
구현된 규칙:
- 마스터 이미지는 여러 개 생성 가능 (씬당 최대 3개) ✅
- Active Master 개념 (isActive 플래그) ✅
- 각 노드는 생성 상태 표시 (PENDING/RUNNING/SUCCEEDED/FAILED) ✅
- 완료된 노드에서 "확정" 버튼 → 타임라인에 확정 ✅
- 삭제 경고 모달 ✅ (NodeDeleteConfirmModal.vue)
```

#### 4.3.2 AI 생성 대기 UX ✅
```
구현된 항목:
- 진행 상태(대기/진행중/완료/실패) 표시 ✅
- 토스트 알림 (완료/실패) ✅
- 백그라운드 생성 지원으로 다른 씬 작업 가능 ✅
```

#### 4.3.3 에러 처리 원칙 ✅
```
구현된 항목:
- 에러 원인 토스트로 명확히 표시 ✅
- 재시도 옵션 제공 ✅
- generateForNode()에서 에러 핸들링 ✅
```

---

## 5. 사용자 플로우 (User Flow) - 구현 현황

### 5.1 전체 플로우 ✅

```
┌──────────────────────────────────────────────────────────────────┐
│ 1️⃣ 기획 단계 ✅                                                   │
│    • 로그인 → 새 프로젝트 생성 ✅ (DashboardPage)                   │
│    • (선택) 팀원 초대 → 플로팅 협업 바로 화상통화 브레인스토밍 ✅     │
│    • AI 시나리오 생성으로 아이디어 구체화 ✅ (scenario store)        │
│    • 오브젝트 시트 생성 ✅ (character store)                        │
│    • 씬 구조 설계 (씬 추가 및 설명 작성) ✅                          │
└──────────────────────────────────────────────────────────────────┘
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│ 2️⃣ 씬 편집 단계 (노드 기반 캔버스) ✅                               │
│    🔷 마스터 노드 생성 ✅                                           │
│    🔷 그리드 노드 생성 ✅                                           │
│    🔷 샷 노드 생성 ✅                                               │
│    🔷 영상 노드 생성 ✅                                             │
│    💡 분기 가능: 각 단계에서 여러 버전 실험 가능 ✅                   │
│    💡 확정 개념: 확정된 영상만 타임라인에 반영 ✅                     │
└──────────────────────────────────────────────────────────────────┘
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│ 3️⃣ 음악 파일 업로드/보관 (P1)                                     │
│    • 외부에서 생성한 배경음악 파일 업로드 ⬜                        │
└──────────────────────────────────────────────────────────────────┘
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│ 4️⃣ 편집/완성 단계 ✅                                               │
│    • 타임라인에서 ★ 확정된 영상 클립들 확인 ✅                      │
│    • 순서 조정 (드래그 앤 드롭) ✅                                  │
│    • "영상 병합" 클릭 → 최종 영상 생성 ✅                           │
│    • 미리보기 → 다운로드 ✅                                        │
└──────────────────────────────────────────────────────────────────┘
```

---

## 6. 화면 구성 - 구현 현황

### 6.1 주요 화면 목록

| 화면 | 설명 | 구현 | 파일 위치 |
|------|------|------|-----------|
| 랜딩 페이지 | 서비스 소개, 로그인/회원가입 CTA | ✅ | `LandingPage.vue` |
| 로그인/회원가입 | 인증 화면 | ✅ | `AuthPage.vue` |
| 대시보드 | 프로젝트 목록, 새 프로젝트 생성 | ✅ | `DashboardPage.vue` |
| 프로젝트 상세 | 시나리오, 씬 목록, **협업 시작 버튼** | ✅ | `ProjectDetailPage.vue` |
| **플로팅 협업 바** | **모든 화면 하단에 오버레이** | ✅ | `CollabContainer.vue`, `CollabPanel.vue` |
| 씬 편집 | **노드 기반 캔버스 (Vue Flow)** | ✅ | `SceneEditPage.vue`, `NodeCanvas.vue` |
| 타임라인/편집 | **확정된 영상만** 표시, 순서 조정, 병합 | ✅ | `TimelinePage.vue` |
| 프로필 | 사용자 프로필 조회/수정 | ✅ | `ProfilePage.vue`, `ProfileEditPage.vue` |
| 즐겨찾기 | 즐겨찾기한 프로젝트 목록 | ✅ | `FavoritesPage.vue` |
| 휴지통 | 삭제된 프로젝트 목록 | ✅ | `TrashPage.vue` |
| 권한 거부 | 접근 권한 없음 안내 | ✅ | `AccessDeniedPage.vue` |

---

## 7. 기술 요구사항 - 구현 현황

### 7.1 프론트엔드 ✅
```
구현 완료:
- Vue 3 + Composition API ✅
- Vue Router (SPA 라우팅) ✅ - src/router/index.ts
- Pinia (상태 관리) ✅ - src/stores/ (15개 store)
- Axios (API 통신) ✅ - src/services/api/client.ts
- Vue Flow (노드 기반 씬 편집 캔버스) ✅
- WebRTC API (화상통화) ✅ - src/stores/collab.ts
- 반응형 디자인 (Desktop 우선) ✅
- WebSocket 클라이언트 ✅ - src/services/ws/

디렉토리 구조:
src/
├── pages/ (12개 페이지 + 1개 하위 디렉토리)
├── components/ (7개 하위 디렉토리, 65개 컴포넌트)
├── stores/ (15개 store 파일)
├── services/
│   ├── api/ (15개 API 모듈)
│   ├── ws/ (WebSocket 모듈)
│   ├── webrtc/ (WebRTC 모듈)
│   └── mock/ (8개 목 데이터)
├── types/ (18개 타입 정의)
├── composables/ (7개 composable)
├── layouts/ (4개 레이아웃)
└── utils/ (2개 유틸리티)
```

### 7.2 주요 Store 구현 현황

| Store | 파일 | 규모 | 역할 |
|-------|------|------|------|
| auth | `auth.ts` | 1.8KB | 인증, 로그인/로그아웃, 토큰 관리 |
| project | `project.ts` | 8.6KB | 프로젝트 CRUD, 썸네일, 필터링 |
| scenario | `scenario.ts` | 17.3KB | 4단계 AI 시나리오 생성 플로우 |
| scene | `scene.ts` | 6.9KB | 씬 CRUD, 순서 관리 |
| sceneNode | `sceneNode/index.ts` | 43KB | 노드 기반 씬 편집 (1179줄) |
| timeline | `timeline.ts` | 11.7KB | 타임라인 클립 관리, 병합 |
| collab | `collab.ts` | 21.5KB | WebRTC 협업, 화상통화, 채팅 (646줄) |
| character | `character.ts` | 4.2KB | 캐릭터/오브젝트 시트 관리 |
| node | `node.ts` | 6.7KB | 노드 공통 유틸리티 |
| ui | `ui.ts` | 2.2KB | 토스트 알림, UI 상태 |

### 7.3 주요 API 모듈 구현 현황

| API | 파일 | 규모 | 역할 |
|-----|------|------|------|
| ai | `ai.ts` | 6KB | AI 생성 요청, 프롬프트 개선 |
| auth | `auth.ts` | 1KB | 인증 API |
| projects | `projects.ts` | 4.3KB | 프로젝트 CRUD |
| scenario | `scenario.ts` | 3KB | 시나리오 생성 API |
| scenes | `scenes.ts` | 2.1KB | 씬 CRUD |
| nodes | `nodes.ts` | 2.3KB | 노드 CRUD, 확정 |
| objects | `objects.ts` | 3KB | 오브젝트 시트 API |
| timeline | `timeline.ts` | 1.2KB | 타임라인, 병합, 내보내기 |
| files | `files.ts` | 1.2KB | 파일 업로드 |

---

## 8. 성공 지표 - 구현 현황

### 8.1 MVP 완성 기준

- [x] 프로젝트/씬 생성 및 관리 가능 ✅
- [x] AI 시나리오 생성 동작 ✅
- [x] Gemini로 스토리보드 이미지 생성 가능 ✅
- [x] Google Veo 3.1로 Image-to-Video 생성 가능 ✅
- [x] WebRTC 화상통화 + 화면공유 동작 ✅
- [x] 씬별 영상 병합하여 최종 영상 생성 가능 ✅
- [x] 전체 워크플로우가 끊김 없이 동작 ✅

---

## 12. 부록

### 12.1 용어 정의

| 용어 | 정의 |
|------|------|
| 프로젝트 | 하나의 AI 영화 작품 단위 |
| 씬 | 프로젝트 내 장면 단위 (스토리보드+영상+음악) |
| 스토리보드 | 씬의 구도를 나타내는 이미지 |
| 노드 | 씬 편집 캔버스의 구성 요소 (씬 헤더/마스터/그리드/샷/영상) |
| 마스터 이미지 | 씬의 기준이 되는 와이드 샷 이미지 (루트 노드) |
| 스토리보드 그리드 | 마스터 기반 여러 샷을 배치한 1장의 이미지 (2x2, 2x3 등) |
| 샷 | 그리드에서 추출한 개별 고품질 이미지 |
| 확정 (Confirm) | 영상 노드를 타임라인에 확정하는 액션 |
| Image-to-Video | 정지 이미지를 동영상으로 변환하는 AI 기술 |
| 플로팅 협업 바 | 모든 화면에서 유지되는 화상통화/화면공유 오버레이 |
| Veo 3.1 | Google Cloud의 Image-to-Video AI 모델 |

### 12.2 프론트엔드 주요 파일 구조

```
itda-frontend/src/
├── pages/
│   ├── AuthPage.vue          # 로그인/회원가입
│   ├── LandingPage.vue       # 랜딩 페이지
│   ├── DashboardPage.vue     # 대시보드
│   ├── ProjectDetailPage.vue # 프로젝트 상세
│   ├── SceneEditPage.vue     # 씬 편집 (노드 캔버스)
│   ├── TimelinePage.vue      # 타임라인
│   ├── ProfilePage.vue       # 프로필
│   ├── ProfileEditPage.vue   # 프로필 수정
│   ├── FavoritesPage.vue     # 즐겨찾기
│   ├── TrashPage.vue         # 휴지통
│   └── AccessDeniedPage.vue  # 권한 거부
│
├── stores/
│   ├── auth.ts               # 인증
│   ├── project.ts            # 프로젝트
│   ├── scenario.ts           # AI 시나리오 (4단계)
│   ├── scene.ts              # 씬
│   ├── sceneNode/            # 노드 기반 편집 (43KB)
│   │   ├── index.ts
│   │   ├── types.ts
│   │   ├── mappers.ts
│   │   ├── edges.ts
│   │   └── history.ts
│   ├── timeline.ts           # 타임라인, 병합
│   ├── collab.ts             # WebRTC 협업 (21.5KB)
│   ├── character.ts          # 오브젝트 시트
│   ├── node.ts               # 노드 유틸
│   └── ui.ts                 # 토스트, UI 상태
│
├── components/
│   ├── scene-editor/
│   │   ├── NodeCanvas.vue
│   │   ├── nodes/            # 6개 노드 컴포넌트
│   │   └── panels/           # 8개 패널 컴포넌트
│   ├── collab/               # 6개 협업 컴포넌트
│   ├── timeline/             # 6개 타임라인 컴포넌트
│   ├── scenario/             # 6개 시나리오 컴포넌트
│   ├── project/              # 8개 프로젝트 컴포넌트
│   ├── common/               # 13개 공통 컴포넌트
│   └── editor/               # 10개 에디터 컴포넌트
│
├── services/
│   ├── api/                  # 15개 API 모듈
│   ├── ws/                   # WebSocket
│   └── webrtc/               # WebRTC
│
└── types/                    # 18개 타입 정의
```

---

## 13. 문서 이력

- v1.0 (2026-01-07): 초안 작성 (AI Movie Studio)
- v1.1 (2026-01-07): AI Shorts Studio로 피벗
- v1.2 (2026-01-08): 시장 조사, 모션 참조 AI, 디렉팅 패널 추가
- v2.0 (2026-01-12): **워크플로우 기반 AI 영화 제작 플랫폼으로 전면 재설계**
- v2.1 (2026-01-12): **MVP(P0) 범위 슬림화**
- v2.2 (2026-01-12): **UX 가이드라인 및 UI 개선**
- v2.3 (2026-01-14): **노드 기반 UI + 플로팅 협업 바 전면 재설계**
- v2.4 (2026-01-14): **기술 스택 및 AI Provider 개편**
- v2.5 (2026-01-14): **노드 시스템 상세 설계**
- v2.5.1 (2026-01-15): **용어 일반화 + AI 시나리오 생성 플로우 개선**
- v2.5.2 (2026-01-15): **노드 워크플로우 개선**
- v2.5.3 (2026-01-24): **2단계 병합 구조**
- **v2.6.0 (2026-01-29): 코드베이스 분석 기반 현행 구현 반영** ⬅️ 현재 버전
  - 프론트엔드 코드베이스 분석을 통해 실제 구현된 기능 확인 및 반영
  - 구현 완료된 기능에 ✅ 표시, 진행 중인 기능에 🔄 표시
  - Store, API, 컴포넌트 구현 현황 상세 문서화
