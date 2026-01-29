# 오브젝트 프론트엔드 작업 리뷰

## 요약
- 서버 구현 기준(멀티파트 이미지 업로드)으로 오브젝트 CRUD + 이미지 교체/다운로드를 프론트에 연결.
- “캐릭터” 용어를 “오브젝트”로 통일(파일/컴포넌트/스토어/텍스트).
- 씬 편집 프롬프트에서 오브젝트 **ID 대신 이름**을 사용하도록 변경.

## 기준 및 범위
- 기준: 현재 백엔드 구현(`POST /api/projects/{id}/objects`는 이미지 파일 필수).
- 포함: 생성/목록/상세/수정/삭제 + 이미지 교체(PATCH) + 이미지 다운로드(GET).
- 제외: 오브젝트 생성 Job/PENDING 흐름(문서상 예정 기능).

## 주요 변경점

### 1) 오브젝트 API 연동 (multipart + 이미지 교체/다운로드)
- `itda-frontend/src/services/api/objects.ts`
  - `createObject`는 `FormData`로 `file + name/type/description/style` 전송.
  - `replaceObjectImage`(PATCH) 추가.
  - `downloadObjectImage`(GET blob) 추가.
- `itda-frontend/src/stores/object.ts`
  - `addObject`, `updateObject`, `removeObject`, `downloadObjectImage` 액션 추가.

### 2) UI(오브젝트 탭) 전면 교체
- `itda-frontend/src/pages/project/sections/ObjectsTab.vue`
  - 카드/모달 교체 및 핸들러 재구성.
- `itda-frontend/src/components/project/AddObjectModal.vue`
  - 이름/타입/설명/스타일/이미지 업로드 필수 입력.
- `itda-frontend/src/components/project/EditObjectModal.vue`
  - 텍스트 수정 + 이미지 교체 지원.
- `itda-frontend/src/components/project/ObjectCard.vue`
  - 다운로드 버튼 추가, 타입 라벨 한글 표시.
- `itda-frontend/src/pages/ProjectDetailPage.vue`
  - 오브젝트 탭 props 및 스타일 클래스 변경.

### 3) 씬 편집 프롬프트 연동
- `itda-frontend/src/components/scene-editor/panels/MasterImagePanel.vue`
  - 오브젝트 목록을 실제 저장된 오브젝트(store)로 교체.
  - 프롬프트 payload는 오브젝트 **이름** 사용.
- `itda-frontend/src/components/scene-editor/panels/ShotPanel.vue`
  - 활성 마스터의 오브젝트 ID를 이름으로 매핑하여 프롬프트에 전달.
- `itda-frontend/src/pages/SceneEditPage.vue`
  - 씬 편집 진입 시 오브젝트 목록 로드.

### 4) 타입 정리
- `itda-frontend/src/types/api/objects.ts`
  - `ObjectStatus`, `UpdateObjectRequest` 추가.
- `itda-frontend/src/types/api/ai.ts`
  - `objectIds` 타입을 `number[]`로 정리.
- `itda-frontend/src/types/ui/sceneNodes.ts`
  - `objectIds` 타입을 `number[]`로 정리.
- `itda-frontend/src/services/api/ai.ts`
  - `objectIds`는 프롬프트 요청 시 문자열 배열로 변환.
- `itda-frontend/src/stores/sceneNode/index.ts`
  - `objectIds` 파싱 시 숫자 정규화.

## 네이밍 변경(캐릭터 → 오브젝트)
- 파일명 변경:
  - `itda-frontend/src/components/project/CharacterCard.vue` → `itda-frontend/src/components/project/ObjectCard.vue`
  - `itda-frontend/src/components/project/AddCharacterModal.vue` → `itda-frontend/src/components/project/AddObjectModal.vue`
  - `itda-frontend/src/services/mock/characters.ts` → `itda-frontend/src/services/mock/objects.ts`
  - `itda-frontend/src/stores/character.ts` → `itda-frontend/src/stores/object.ts`

## 동작 흐름 요약
1) 오브젝트 추가
   - 이름/유형/설명/스타일/이미지 선택 → `POST /api/projects/{id}/objects` (multipart)
2) 오브젝트 수정
   - 텍스트 수정 → `PUT /api/objects/{id}`
   - 이미지 교체 선택 시 → `PATCH /api/objects/{id}/image`
3) 오브젝트 다운로드
   - 카드에서 다운로드 클릭 → `GET /api/objects/{id}/image` (blob)
4) 씬 편집 프롬프트
   - 오브젝트 선택은 ID로 저장, AI 프롬프트에는 **오브젝트 이름** 전달

## 확인 포인트(수동 테스트 체크리스트)
- 오브젝트 생성: 이미지 필수 여부 검증 + 생성 후 카드 노출.
- 오브젝트 수정: 텍스트만 수정 / 텍스트+이미지 교체 둘 다 동작.
- 오브젝트 다운로드: 이미지 파일 다운로드 가능 여부.
- 씬 편집: “등장 오브젝트” 리스트가 실제 오브젝트 목록으로 보이는지.
- 프롬프트 생성: 선택한 오브젝트 이름이 prompt에 포함되는지.

## 비고 / 리스크
- 문서의 Job/PENDING 흐름은 미적용(서버 구현 기준).
- 오브젝트 수정 시 이미지 교체는 **별도 PATCH** 요청으로 처리됨(서버 기준).
