# API 연동규격서

> **프로젝트**: AI Movie Studio  
> **버전**: v1.0  
> **날짜**: 2026-01-24  

> **W3 Contract Freeze (2026-01-24)**: `docs/w3-mvp-implementation-plan.md` Section 2 기준.  
> W3 WS는 **STOMP `/ws` + `/topic/projects/{projectId}`**, W5에 Raw WS(`/ws/room/{roomId}`, `/ws/projects/{projectId}`) 확장.

---

## 0. API 요약 (W3 기준)

### 인증 (3)
- 회원가입: `POST /api/auth/signup`
- 로그인: `POST /api/auth/login`
- 내 정보 조회: `GET /api/auth/me`

### 프로젝트 (5)
- 프로젝트 생성: `POST /api/projects`
- 프로젝트 목록: `GET /api/projects`
- 프로젝트 상세: `GET /api/projects/{id}`
- 프로젝트 수정: `PUT /api/projects/{id}`
- 프로젝트 삭제: `DELETE /api/projects/{id}`

### 씬 (4)
- 씬 생성: `POST /api/projects/{id}/scenes`
- 씬 목록: `GET /api/projects/{id}/scenes`
- 씬 상세: `GET /api/scenes/{id}`
- 씬 순서 변경: `PUT /api/projects/{id}/scenes/order`

### 시나리오 (4)
- 시나리오 조회: `GET /api/projects/{id}/scenario`
- 프롬프트 생성: `POST /api/projects/{id}/scenario/prompt/generate`
- 플롯 생성: `POST /api/projects/{id}/scenario/plot/generate`
- 씬 생성: `POST /api/projects/{id}/scenario/scenes/generate`

### 노드/캔버스 (6)
- 노드 생성: `POST /api/scenes/{sceneId}/nodes`
- 노드 목록: `GET /api/scenes/{sceneId}/nodes`
- 노드 수정: `PUT /api/nodes/{id}`
- 영상 확정: `POST /api/nodes/{id}/confirm`
- 확정 취소: `DELETE /api/nodes/{id}/confirm`
- 결과 생성: `POST /api/nodes/{id}/generate`

### AI/프롬프트/작업 (3)
- 프롬프트 생성: `POST /api/ai/prompts/generate`
- 프롬프트 개선: `POST /api/ai/prompts/improve`
- 작업 상태 조회: `GET /api/ai/jobs/{jobId}`

### 타임라인/병합/내보내기 (3)
- 프로젝트 타임라인 조회: `GET /api/projects/{id}/timeline`
- 최종 병합 요청: `POST /api/projects/{id}/merge`
- 내보내기 URL 조회: `GET /api/projects/{id}/export`

### WebSocket (W3)
- STOMP `/ws` + `/topic/projects/{projectId}`

### W4~W5 예정 (범위 외)
- 멤버/권한, 오브젝트 시트, 파일 업로드
- 협업(WebRTC/Chat/Presence) 및 Raw WS `/ws/room/{roomId}`, `/ws/projects/{projectId}`

상세 엔드포인트는 각 섹션에서 확인합니다.

## 1. 개요
본 문서는 AI Movie Studio 프로젝트의 서버 API 연동 규격을 정의합니다.
클라이언트(Front-end)와 서버(Back-end) 간의 데이터 통신을 위한 약속으로 사용됩니다.

### 1.1 공통 규칙

- **인증**: `Authorization: Bearer <JWT>`
- **페이지네이션(목록 API)**: `?page=0&size=20` (기본 0/20)
- **중복 요청 방지(선택)**: `Idempotency-Key` 헤더를 지원하여 동일 키로 재요청 시 동일 `jobId`를 반환

### 1.2 공통 응답 포맷
적용 범위: 모든 JSON REST API 응답은 본 공통 포맷을 사용합니다.
예외: 비-JSON 응답(파일 다운로드/스트리밍 등)과 WebSocket 이벤트만 제외합니다.
`code`는 도메인 문자열 코드이며, HTTP 상태 코드는 별도로 사용합니다. (예: SUCCESS, EMAIL_ALREADY_EXISTS)
성공 code 값: SUCCESS(즉시 성공), ACCEPTED(비동기 작업 접수).
예시: 가독성을 위해 일부 응답 예시는 `message`를 생략할 수 있습니다.

#### 공통 응답 필드 정의
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| code | String | 필수 | 도메인 코드 (SUCCESS, EMAIL_ALREADY_EXISTS 등) |
| message | String | 선택 | 사용자/로그용 메시지 |
| data | Object\|Array | 선택 | 성공 시 응답 데이터 |
| details | Object | 선택 | 실패 시 상세 정보 |

※ `details`는 실패 시에만 사용하며, 필요 없으면 생략합니다.

#### 성공 시 (기본)
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... }
}
```

#### 실패 시 (에러)
```json
{
  "code": "FORBIDDEN",
  "message": "You do not have permission to edit this project.",
  "details": {
    "projectId": 123
  }
}
```

#### 도메인 에러 코드 (예시)
| 코드 | 설명 |
| --- | --- |
| UNAUTHORIZED | 인증 필요 |
| FORBIDDEN | 권한 없음 |
| USER_NOT_FOUND | 사용자 없음 |
| EMAIL_ALREADY_EXISTS | 이미 가입된 이메일 |
| PROJECT_NOT_FOUND | 프로젝트 없음 |
| SCENE_NOT_FOUND | 씬 없음 |
| SCENE_LIMIT_EXCEEDED | 씬 개수 제한 초과 |
| NODE_NOT_FOUND | 노드 없음 |
| JOB_NOT_FOUND | 작업 없음 |
| INVALID_REQUEST | 요청 파라미터 오류 |
| MERGE_FAILED | 프로젝트 병합 실패 |

### 1.3 비동기 AI 작업 응답 규칙
AI 생성 작업(이미지/영상)은 비동기로 처리됩니다.
1. 생성 요청은 `202 Accepted` + `jobId` 반환
2. 클라이언트는 **프로젝트 이벤트 WebSocket(2.10)** 으로 완료/실패 이벤트를 받고, 필요 시 `GET /api/ai/jobs/{jobId}`로 결과/에러를 조회
3. (Fallback) WebSocket 연결이 없으면 `GET /api/ai/jobs/{jobId}` 폴링으로 상태를 확인
4. `progress`(%)는 AI Provider에 따라 **미지원일 수 있으며**, MVP에서는 status 기반 UI를 기본으로 함

```json
{
  "code": "SUCCESS",
  "data": {
    "jobId": 12345,
    "type": "VIDEO_GENERATION",
    "status": "RUNNING",
    "progress": null,
    "target": { "type": "NODE", "id": 987 },
    "resultUrl": null,
    "error": null
  }
}
```

---

## 2. API 상세

### 2.1 인증 API (Authentication)

# 회원가입
```
API /api/auth/signup
메서드 POST
보안 None
상태 완료
설명 사용자가 새로운 계정을 등록합니다.
```

#### 1. API 개요
```
API 경로 : /api/auth/signup
메서드 : POST
설명 : 사용자가 이메일과 비밀번호 등을 입력하여 서비스에 가입합니다.
```

#### 2. Request

#### 요청 형식
```json
{
  "email": "String", // 사용자 이메일 (필수, ID 역할)
  "password": "String", // 비밀번호 (필수)
  "name": "String" // 사용자 이름 (필수)
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| email | String | 필수 | 사용자 이메일 (로그인 ID) |
| password | String | 필수 | 사용자 비밀번호 |
| name | String | 필수 | 사용자 이름 |

#### 3. Response

#### 성공 시
```json
{
  "code": "SUCCESS",
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "profileImageUrl": null,
    "role": "USER"
  }
}
```

#### 실패 시
```json
{
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "이미 가입된 이메일입니다."
}
```

#### 4. 설명
- 이메일 중복 체크를 수행합니다.
- 패스워드는 서버에서 암호화되어 저장됩니다.

---

# 로그인
```
API /api/auth/login
메서드 POST
보안 None
상태 완료
설명 사용자가 이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.
```

#### 1. API 개요
```
API 경로 : /api/auth/login
메서드 : POST
설명 : 로그인 성공 시 액세스 토큰(JWT)과 갱신 토큰을 반환합니다.
```

#### 2. Request

#### 요청 형식
```json
{
  "email": "String", // 이메일 (필수)
  "password": "String" // 비밀번호 (필수)
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| email | String | 필수 | 사용자 이메일 |
| password | String | 필수 | 사용자 비밀번호 |

#### 3. Response

#### 성공 시
```json
{
  "code": "SUCCESS",
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1Ni...",
    "expiresIn": 3600,
    "refreshToken": "eyJhbGciOiJIUzI1Ni..."
  }
}
```

---

# 로그아웃
```
API /api/auth/logout
메서드 POST
보안 Bearer Token
상태 완료
설명 사용자의 세션을 종료하고 토큰을 만료시킵니다 (Server-side blacklist 등).
```

#### 1. API 개요
```
API 경로 : /api/auth/logout
메서드 : POST
인증 : Bearer Token 필요
```

---

# 내 정보 조회
```
API /api/auth/me
메서드 GET
보안 Bearer Token
상태 완료
설명 현재 로그인한 사용자의 프로필 정보를 조회합니다.
```

#### 1. API 개요
```
API 경로 : /api/auth/me
메서드 : GET
설명 : JWT 토큰을 기반으로 사용자 정보를 식별하여 반환합니다.
```

#### 3. Response (성공)
```json
{
  "code": "SUCCESS",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "profileImageUrl": "https://s3.aws.com/...",
    "role": "USER"
  }
}
```

---

# 내 프로필 수정
```
API /api/auth/me
메서드 PUT
보안 Bearer Token
상태 완료
설명 현재 사용자의 프로필 정보(이름, 프로필 이미지)를 수정합니다.
```

#### 2. Request
```json
{
  "name": "New Name",
  "profileImage": "https://new-image-url..."
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| name | String | 선택 | 변경할 사용자 이름 |
| profileImage | String | 선택 | 변경할 프로필 이미지 URL |

---

# 비밀번호 재설정 요청
```
API /api/auth/password/reset/request
메서드 POST
보안 None
상태 완료
설명 가입된 이메일로 비밀번호 재설정 링크 또는 코드를 발송합니다.
```

#### 2. Request
```json
{
  "email": "user@example.com"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| email | String | 필수 | 가입된 사용자 이메일 |

---

# 비밀번호 재설정 완료
```
API /api/auth/password/reset/confirm
메서드 POST
보안 None
상태 완료
설명 이메일로 받은 토큰/코드와 새로운 비밀번호를 입력하여 재설정을 완료합니다.
```

#### 2. Request
```json
{
  "token": "reset_token_123",
  "newPassword": "newSecretPassword!@"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| token | String | 필수 | 이메일로 발송된 재설정 토큰/코드 |
| newPassword | String | 필수 | 설정할 새로운 비밀번호 |

---

# 구글 소셜 로그인 (P1)
```
API /api/auth/oauth/google
메서드 POST
보안 None
상태 완료
설명 구글 OAuth 인증 후 액세스 토큰을 발급받습니다.
```

---

### 2.2 프로젝트 API (Projects)

# 프로젝트 생성
```
API /api/projects
메서드 POST
보안 Bearer Token
상태 완료
설명 새로운 영화 제작 프로젝트를 생성합니다.
```

#### 1. API 개요
```
API 경로 : /api/projects
메서드 : POST
설명 : 생성자는 자동으로 프로젝트의 Owner가 됩니다.
```

#### 2. Request
```json
{
  "title": "화성 브이로그", // (필수)
  "description": "화성에서 살아남기...", // (선택)
  "genre": "SF" // (선택)
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| title | String | 필수 | 프로젝트 제목 |
| description | String | 선택 | 프로젝트 설명 |
| genre | String | 선택 | 프로젝트 장르 (예: SF, COMEDY) |

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "projectId": 101,
    "title": "화성 브이로그",
    "role": "OWNER",
    "createdAt": "2026-01-15T..."
  }
}
```

---

# 프로젝트 목록 조회
```
API /api/projects
메서드 GET
보안 Bearer Token
상태 완료
설명 내가 참여 중인(Owner, Editor, Viewer) 모든 프로젝트 목록을 조회합니다.
```

#### 1. API 개요
```
API 경로 : /api/projects
메서드 : GET
쿼리 : ?page=0&size=20 (페이지네이션)
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "items": [
      {
        "projectId": 101,
        "title": "화성 브이로그",
        "thumbnailUrl": "https://...",
        "role": "OWNER",
        "memberCount": 3,
        "sceneCount": 5,
        "updatedAt": "..."
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1
  }
}
```

---

# 프로젝트 상세 조회
```
API /api/projects/{id}
메서드 GET
보안 Bearer Token
상태 완료
설명 특정 프로젝트의 상세 정보와 메타데이터를 조회합니다. (씬 목록은 별도 조회)
```

#### 1. API 개요
```
API 경로 : /api/projects/{id}
메서드 : GET
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "projectId": 101,
    "title": "화성 브이로그",
    "description": "...",
    "genre": "SF",
    "myRole": "OWNER",
    "ownerId": 1,
    "members": [ ... ], // 간단 요약
    "createdAt": "..."
  }
}
```

---

# 프로젝트 수정
```
API /api/projects/{id}
메서드 PUT
보안 Bearer Token (Owner)
상태 완료
설명 프로젝트의 메타데이터(제목, 설명 등)를 수정합니다.
```

---

# 프로젝트 삭제
```
API /api/projects/{id}
메서드 DELETE
보안 Bearer Token (Owner)
상태 완료
설명 프로젝트를 영구적으로 삭제합니다. 포함된 씬과 노드들도 모두 삭제됩니다.
```

---

# 멤버 목록 조회
```
API /api/projects/{id}/members
메서드 GET
보안 Bearer Token
상태 완료
설명 프로젝트에 참여 중인 멤버 목록과 권한을 조회합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": [
    {
      "userId": 1,
      "email": "owner@test.com",
      "name": "Owner",
      "role": "OWNER",
      "profileImage": "..."
    },
    {
      "userId": 2,
      "name": "Kim",
      "role": "EDITOR"
    }
  ]
}
```

---

# 멤버 초대 (추가)
```
API /api/projects/{id}/members
메서드 POST
보안 Bearer Token (Owner)
상태 완료
설명 이미 가입된 사용자를 이메일로 검색하여 프로젝트 멤버로 추가합니다.
```

#### 2. Request
```json
{
  "email": "user@example.com",
  "role": "EDITOR" // EDITOR 또는 VIEWER
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| email | String | 필수 | 초대할 사용자 이메일 |
| role | String | 필수 | 멤버 권한 (EDITOR \\| VIEWER) |

---

# 멤버 권한 변경
```
API /api/projects/{id}/members/{memberId}
메서드 PATCH
보안 Bearer Token (Owner)
상태 완료
설명 특정 멤버의 권한(Role)을 변경합니다.
```

#### 2. Request
```json
{
  "role": "VIEWER"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| role | String | 필수 | 변경할 권한 (EDITOR \\| VIEWER) |

---

# 멤버 강퇴
```
API /api/projects/{id}/members/{memberId}
메서드 DELETE
보안 Bearer Token (Owner)
상태 완료
설명 특정 멤버를 프로젝트에서 제외시킵니다.
```

---

# 프로젝트 탈퇴
```
API /api/projects/{id}/members/me
메서드 DELETE
보안 Bearer Token
상태 완료
설명 스스로 프로젝트에서 나갑니다. (Owner는 불가)
```

### 2.3 오브젝트 시트 API (Object Sheets)

# 오브젝트 생성
```
API /api/projects/{id}/objects
메서드 POST
보안 Bearer Token
상태 완료
설명 오브젝트(캐릭터/소품)의 정보를 입력하여 시트 이미지를 생성합니다.
```

#### 2. Request
```json
{
  "name": "우주인 민준",
  "type": "CHARACTER", // CHARACTER, PROP, ETC
  "description": "20대 후반 남성, 우주복 착용, 헬멧 벗음",
  "style": "SF 실사"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| name | String | 필수 | 오브젝트 이름 |
| type | String | 필수 | 오브젝트 유형 (CHARACTER, PROP 등) |
| description | String | 필수 | 외형 설명 |
| style | String | 선택 | 아트 스타일 |

#### 3. Response (현재 OBJ-1 기준)
```json
{
  "code": "SUCCESS",
  "data": {
    "objectId": 101,
    "projectId": 12,
    "name": "우주인 민준",
    "type": "CHARACTER",
    "description": "20대 후반 남성, 우주복 착용, 헬멧 벗음",
    "style": "SF 실사",
    "sheetImageUrl": null,
    "status": "PENDING"
  }
}
```

#### 응답 필드 설명
| 필드 | 타입 | 설명 |
| --- | --- | --- |
| objectId | Long | 오브젝트 ID |
| projectId | Long | 프로젝트 ID |
| name | String | 오브젝트 이름 |
| type | String | 오브젝트 유형 (CHARACTER, PROP, ETC) |
| description | String | 외형 설명 |
| style | String | 아트 스타일 (선택) |
| sheetImageUrl | String | 시트 이미지 URL (생성 완료 전 null 가능) |
| status | String | 생성 상태 (PENDING/RUNNING/SUCCEEDED/FAILED) |

> OBJ-2 전환 시 `202 Accepted + jobId` 응답으로 변경 예정

---

# 오브젝트 목록 조회
```
API /api/projects/{id}/objects
메서드 GET
보안 Bearer Token
상태 완료
설명 프로젝트에 등록된 모든 오브젝트 목록을 조회합니다. (페이지네이션 미적용, 전체 반환)
```

---

# 오브젝트 상세 조회
```
API /api/objects/{id}
메서드 GET
보안 Bearer Token
상태 완료
```

---

# 오브젝트 수정
```
API /api/objects/{id}
메서드 PUT
보안 Bearer Token
상태 완료
설명 오브젝트의 정보(이름, 설명 등)를 수정합니다.
```

#### 2. Request
```json
{
  "name": "수정된 이름",
  "description": "수정된 설명"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| name | String | 선택 | 변경할 오브젝트 이름 |
| description | String | 선택 | 변경할 설명 |

---

# 오브젝트 삭제
```
API /api/objects/{id}
메서드 DELETE
보안 Bearer Token
상태 완료
```

---

### 2.4 씬 API (Scenes)

# 씬 생성
```
API /api/projects/{id}/scenes
메서드 POST
보안 Bearer Token
상태 완료
설명 프로젝트에 새로운 씬을 추가합니다.
```

#### 2. Request
```json
{
  "title": "Scene 1: 화성 기지",
  "description": "주인공이 아침에 일어나는 장면"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| title | String | 필수 | 씬 제목 |
| description | String | 선택 | 씬 설명 |

---

# 씬 목록 조회
```
API /api/projects/{id}/scenes
메서드 GET
보안 Bearer Token
상태 완료
설명 프로젝트의 모든 씬 목록을 순서대로 조회합니다. (전체 반환)
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": [
    {
      "sceneId": 201,
      "title": "Scene 1",
      "order": 1,
      "status": "COMPLETED"
    },
    {
      "sceneId": 202,
      "title": "Scene 2",
      "order": 2
    }
  ]
}
```

---

# 씬 상세 조회
```
API /api/scenes/{id}
메서드 GET
보안 Bearer Token
상태 완료
설명 씬의 상세 정보와 포함된 등장 오브젝트 내역 등을 조회합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "sceneId": 201,
    "projectId": 101,
    "title": "Scene 1: Mars Base",
    "description": "Morning at the base",
    "order": 1,
    "objectIds": [1, 2, 3]
  }
}
```

---

# 씬 수정
```
API /api/scenes/{id}
메서드 PUT
보안 Bearer Token
상태 완료
설명 씬의 제목, 설명, 등장 오브젝트 등을 수정합니다.
```

#### 2. Request
```json
{
  "title": "수정된 제목",
  "description": "수정된 설명",
  "objectIds": [1, 2, 3] // 등장 오브젝트 ID 목록
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| title | String | 선택 | 변경할 씬 제목 |
| description | String | 선택 | 변경할 씬 설명 |
| objectIds | List<Long> | 선택 | 등장 오브젝트 ID 목록 |

---

# 씬 삭제
```
API /api/scenes/{id}
메서드 DELETE
보안 Bearer Token
상태 완료
설명 씬을 삭제합니다. 포함된 노드들도 함께 삭제됩니다. 삭제 후 순서 정리는 /api/projects/{id}/scenes/order 호출로 처리합니다.
```

---

# 씬 순서 변경
```
API /api/projects/{id}/scenes/order
메서드 PUT
보안 Bearer Token
상태 완료
설명 씬들의 순서를 일괄 변경합니다.
```

#### 2. Request
```json
{
  "orderedSceneIds": [202, 201, 203]
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| orderedSceneIds | List<Long> | 필수 | 정렬된 씬 ID 목록 |

---

### 2.5 노드 API (Nodes & Canvas)
노드 유형: SCENE_HEADER(자동), MASTER, GRID, SHOT, VIDEO

#### 제한 규칙
- SCENE_HEADER는 생성/삭제/이동/수정 불가 (positions 저장 요청은 무시)
- 씬당 SCENE_HEADER는 1개만 존재하며, 씬 제목/설명은 Scene API 변경 시 반영
- MASTER 노드는 씬당 최대 3개
- Active Master는 씬당 1개만 유지
- SHOT 아래 VIDEO 확정은 최대 1개만 허용 (확정 시 기존 확정 자동 해제)

# 노드 생성 (마스터/그리드/샷/영상)
```
API /api/scenes/{sceneId}/nodes
메서드 POST
보안 Bearer Token
상태 완료
설명 씬 캔버스에 새로운 노드를 생성합니다. (트리 구조)
※ 노드 생성 시 AI 작업은 시작되지 않습니다. 결과 생성은 별도 API로 요청합니다.
```
※ 씬 헤더 노드는 씬 생성 시 서버가 자동 생성하며, 별도 생성 API는 제공하지 않습니다.

#### 2. Request
##### MASTER 노드 예시
```json
{
  "type": "MASTER",
  "parentNodeId": null,
  "prompt": "화성 기지에서의 아침 식사 풍경",
  "settings": {
    "style": "CINEMATIC",
    "ratio": "16:9"
  }
}
```

##### VIDEO 노드 예시
```json
{
  "type": "VIDEO",
  "parentNodeId": 501,
  "prompt": "우주인이 창밖을 바라보다 고개를 돌린다",
  "settings": {
    "startShotNodeId": 501, // VIDEO 시작 샷 노드 ID (P0)
    "endShotNodeId": null, // VIDEO 종료 샷 노드 ID (P1)
    "cameraMotion": "ZOOM_IN", // ZOOM_IN, ZOOM_OUT, PAN, TILT, STATIC
    "duration": 5, // 영상 길이(초)
    "motionDescription": "우주인이 창밖을 바라보다 고개를 돌린다", // 선택
    "provider": "VEO_3_1" // P0 기본, P1 확장(모델 선택)
  }
}
```
#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| type | String | 필수 | 노드 타입 (MASTER, GRID, SHOT, VIDEO) - SCENE_HEADER는 서버 자동 생성 |
| parentNodeId | Long | 선택 | 부모 노드 ID (루트 노드인 경우 null) |
| prompt | String | 선택 | 초기 프롬프트 |
| settings | Object | 선택 | 노드 설정 (스타일, 비율 등) |
| settings.startShotNodeId | Long | 필수(VIDEO) | VIDEO 시작 샷 노드 ID |
| settings.endShotNodeId | Long | 선택(VIDEO, | P1) VIDEO 종료 샷 노드 ID |
| settings.cameraMotion | String | 필수(VIDEO) | 카메라 모션 (ZOOM_IN, ZOOM_OUT, PAN, TILT, STATIC) |
| settings.duration | Integer | 필수(VIDEO) | 영상 길이(초) |
| settings.motionDescription | String | 선택(VIDEO) | 모션 상세 설명 |
| settings.provider | String | 선택(P1) | 영상 모델 선택 (기본: VEO_3_1) |
※ VIDEO 노드는 `parentNodeId`가 `settings.startShotNodeId`와 반드시 동일해야 합니다.

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "nodeId": 301
  }
}
```

---

# 노드 결과 생성
```
API /api/nodes/{id}/generate
메서드 POST
보안 Bearer Token
상태 완료
설명 승인된 프롬프트를 기반으로 결과 생성을 시작합니다. (비동기 Job)
```

#### 2. Request
```json
{
  "prompt": "화성 기지에서의 아침 식사 풍경",
  "settings": {
    "style": "CINEMATIC",
    "ratio": "16:9"
  },
  "referenceObjectIds": [1, 2, 3]
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| prompt | String | 선택 | 프롬프트 |
| settings | Object | 선택 | 생성 옵션 |
| referenceObjectIds | List<Long> | 선택 | 레퍼런스 오브젝트 ID 목록 (노드 편집에서 선택 시에만 전달) |

#### 3. Response (Job Accepted)
```json
{
  "code": "ACCEPTED",
  "data": {
    "jobId": 456,
    "nodeId": 301,
    "status": "PENDING"
  }
}
```

---

# 씬 노드 트리 조회
```
API /api/scenes/{sceneId}/nodes
메서드 GET
보안 Bearer Token
상태 완료
설명 씬에 포함된 모든 노드 정보를 트리 구조 또는 리스트로 반환합니다. (씬 헤더 노드 포함)
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "nodes": [
      {
        "nodeId": 300,
        "type": "SCENE_HEADER",
        "title": "씬 1",
        "description": "주인공이 아침을 맞이하는 장면",
        "position": { "x": 0, "y": -200 }
      },
      {
        "nodeId": 301,
        "type": "MASTER",
        "isActive": true,
        "status": "SUCCEEDED",
        "contentUrl": "https://...",
        "position": { "x": 0, "y": 0 }
      },
      {
        "nodeId": 302,
        "type": "GRID",
        "parentNodeId": 301
      }
    ]
  }
}
```

---

# 노드 상세 조회
```
API /api/nodes/{id}
메서드 GET
보안 Bearer Token
상태 완료
설명 개별 노드의 상세 설정 및 생성된 이미지/영상 정보를 조회합니다.
```

---

# 노드 수정
```
API /api/nodes/{id}
메서드 PUT
보안 Bearer Token
상태 완료
설명 노드의 프롬프트나 설정을 수정합니다. (재생성 전 단계)
```

#### 2. Request
```json
{
  "prompt": "수정할 프롬프트",
  "settings": {
    "ratio": "16:9"
  }
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| prompt | String | 선택 | 수정할 프롬프트 |
| settings | Object | 선택 | 수정할 노드 설정 |

---

# 노드 위치 일괄 저장
```
API /api/scenes/{sceneId}/nodes/positions
메서드 PUT
보안 Bearer Token
상태 완료
설명 캔버스 내 노드들의 좌표(x, y)를 일괄 저장합니다.
```
※ SCENE_HEADER position 변경 요청은 무시됩니다.

#### 2. Request
```json
{
  "positions": [
    { "nodeId": 301, "x": 100, "y": 200 },
    { "nodeId": 302, "x": 300, "y": 200 }
  ]
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| positions | List<Object> | 필수 | 노드 위치 정보 목록 |
| positions[].nodeId | Long | 필수 | 노드 ID |
| positions[].x | Number | 필수 | X 좌표 |
| positions[].y | Number | 필수 | Y 좌표 |

---

# 노드 삭제
```
API /api/nodes/{id}
메서드 DELETE
보안 Bearer Token
상태 완료
설명 노드를 삭제합니다. 자식 노드가 있다면 함께 삭제됩니다.
```
※ SCENE_HEADER는 삭제할 수 없습니다. (400/403)

---

# 노드 재생성 (새 버전)
```
API /api/nodes/{id}/regenerate
메서드 POST
보안 Bearer Token
상태 완료
설명 기존 노드의 설정을 바탕으로 새로운 버전(Sibling) 노드를 생성하고 AI 작업을 요청합니다.
```
※ VIDEO 노드 재생성 시 settings.startShotNodeId/endShotNodeId/provider 규칙을 동일하게 적용합니다.

#### 2. Request
```json
{
  "prompt": "수정된 프롬프트...",
  "settings": { ... }
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| prompt | String | 선택 | 수정된 프롬프트 (새 버전용) |
| settings | Object | 선택 | 노드 설정 |

#### 3. Response (Job Accepted)
```json
{
  "code": "ACCEPTED",
  "data": {
    "jobId": 789,
    "newNodeId": 789,
    "status": "PENDING"
  }
}
```

---

# Active Master 변경
```
API /api/nodes/{id}/activate
메서드 POST
보안 Bearer Token
상태 완료
설명 (Master 노드) 해당 마스터 노드를 씬의 Active Master로 설정합니다. 동일 씬에서 Active Master는 1개만 유지되며, 새로 활성화 시 기존 Active는 자동 해제됩니다.
```

---

# 영상 확정 (Confirm)
```
API /api/nodes/{id}/confirm
메서드 POST
보안 Bearer Token
상태 완료
설명 VIDEO 타입 노드를 타임라인에 사용할 영상으로 확정합니다. 동일 SHOT에서 확정 VIDEO는 1개만 유지되며, 새로 확정 시 기존 확정은 자동 해제됩니다.
```
※ 동일 SHOT 아래 기존 확정이 있으면 자동 해제됩니다.

# 영상 확정 취소
```
API /api/nodes/{id}/confirm
메서드 DELETE
보안 Bearer Token
상태 완료
설명 확정된 영상 노드의 확정 상태를 해제합니다.
```

---

# 노드 결과 다운로드
```
API /api/nodes/{id}/export
메서드 GET
보안 Bearer Token
상태 완료
설명 생성 완료된 노드(이미지/영상)의 다운로드 URL을 발급받습니다.
```

### 2.6 AI 생성 API (Generation & Prompt)

### 2.6.1 프로젝트 시나리오 API (Scenario)
프로젝트 단위로 **시나리오 결과(프롬프트/줄거리/씬)** 를 저장하고 재조회합니다.

#### 시나리오-씬 동기화 규칙
- 시나리오 씬 항목은 고유 식별자 `scenarioSceneId`를 갖습니다. (order 변경과 무관)
- `sceneId`는 Scene 생성 이후에만 매핑되며, 생성 직후에는 `null`일 수 있습니다.
- [씬 생성] 시점에 Scene API로 씬을 생성한 뒤, `sceneId`를 시나리오 씬 항목에 매핑합니다.
- `sceneId`가 있는 항목은 씬 제목/설명/순서가 서로 동기화됩니다. (최신 수정 기준)
- 씬 삭제 시 해당 시나리오 씬 항목도 제거하거나 `sceneId`를 해제합니다.
- 시나리오 씬 순서 변경은 `/api/projects/{id}/scenario/scenes/order`로 처리하며, `sceneId`가 있는 항목은 `/api/projects/{id}/scenes/order`에도 동일하게 반영합니다.

# 프로젝트 시나리오 조회
```
API /api/projects/{id}/scenario
메서드 GET
보안 Bearer Token
상태 완료
설명 프로젝트에 저장된 시나리오 데이터(프롬프트/줄거리/씬)를 조회합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "projectId": 101,
    "version": 3,
    "currentStep": "SCENES",
    "input": {
      "genre": "SF",
      "mood": "HOPEFUL",
      "sceneCount": 5,
      "keywords": "화성, 생존, 가족",
      "characterHints": "외로운 우주인",
      "backgroundHints": "화성 기지",
      "referenceStyle": "인터스텔라"
    },
    "prompt": {
      "text": "화성에 홀로 남겨진 우주인의 이야기...",
      "status": "APPROVED"
    },
    "plot": {
      "text": "화성 탐사 기지의 마지막 생존자 민준은...",
      "status": "APPROVED"
    },
    "scenes": [
      { "scenarioSceneId": 1001, "sceneId": 201, "order": 1, "title": "조난", "description": "..." },
      { "scenarioSceneId": 1002, "sceneId": 202, "order": 2, "title": "교신 시도", "description": "..." }
    ],
    "updatedAt": "2026-01-15T14:00:00+09:00"
  }
}
```

---

# 시나리오 프롬프트 생성 (저장)
```
API /api/projects/{id}/scenario/prompt/generate
메서드 POST
보안 Bearer Token
상태 완료
설명 입력값 기반으로 시나리오 프롬프트를 생성하고 프로젝트에 저장합니다.
```

#### 2. Request
```json
{
  "genre": "SF",
  "mood": "HOPEFUL",
  "keywords": "화성, 생존, 가족",
  "sceneCount": 5,
  "characterHints": "외로운 우주인, 반항하는 로봇",
  "backgroundHints": "화성 기지, 붉은 사막",
  "referenceStyle": "인터스텔라 느낌"
}
```
※ `keywords`는 **콤마 구분 문자열**로 전달하며, 서버에서 trim/split 처리합니다.

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "prompt": {
      "text": "화성에 홀로 남겨진 우주인이...",
      "status": "DRAFT"
    }
  }
}
```

---

# 시나리오 프롬프트 저장/승인
```
API /api/projects/{id}/scenario/prompt
메서드 PUT
보안 Bearer Token
상태 완료
설명 프롬프트 텍스트를 저장하고 승인 상태를 갱신합니다.
```

#### 2. Request
```json
{
  "text": "수정된 프롬프트...",
  "status": "APPROVED"
}
```

---

# 전체 줄거리 생성 (저장)
```
API /api/projects/{id}/scenario/plot/generate
메서드 POST
보안 Bearer Token
상태 완료
설명 승인된 프롬프트를 바탕으로 전체 줄거리를 생성하고 저장합니다.
```

요청 바디 없음 (서버에 저장된 승인 프롬프트 사용)

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "plot": {
      "text": "화성 탐사 기지의 마지막 생존자 민준은...",
      "status": "DRAFT"
    }
  }
}
```

---

# 전체 줄거리 저장/승인
```
API /api/projects/{id}/scenario/plot
메서드 PUT
보안 Bearer Token
상태 완료
설명 전체 줄거리 텍스트를 저장하고 승인 상태를 갱신합니다.
```

#### 2. Request
```json
{
  "text": "수정된 줄거리...",
  "status": "APPROVED"
}
```

---

# 씬 스토리 생성 (저장)
```
API /api/projects/{id}/scenario/scenes/generate
메서드 POST
보안 Bearer Token
상태 완료
설명 승인된 줄거리를 기반으로 씬별 제목/스토리를 생성하고 저장합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "scenes": [
      { "scenarioSceneId": 1001, "sceneId": null, "order": 1, "title": "조난", "description": "..." },
      { "scenarioSceneId": 1002, "sceneId": null, "order": 2, "title": "교신 시도", "description": "..." }
    ],
    "status": "DRAFT"
  }
}
```

---

# 씬 스토리 전체 저장
```
API /api/projects/{id}/scenario/scenes
메서드 PUT
보안 Bearer Token
상태 완료
설명 씬 목록을 전체 저장(치환)합니다.
```

#### 2. Request
```json
{
  "scenes": [
    { "scenarioSceneId": 1001, "sceneId": 201, "order": 1, "title": "조난", "description": "..." },
    { "scenarioSceneId": 1002, "sceneId": 202, "order": 2, "title": "교신 시도", "description": "..." }
  ],
  "status": "APPROVED"
}
```
※ 기존 항목 갱신 시 `scenarioSceneId`는 필수이며, 신규 항목 추가는 아래 POST API를 사용합니다.

---

# 씬 스토리 항목 추가
```
API /api/projects/{id}/scenario/scenes
메서드 POST
보안 Bearer Token
상태 완료
설명 시나리오 씬 항목을 추가하고 `scenarioSceneId`를 발급합니다.
```

#### 2. Request
```json
{
  "order": 3,
  "title": "새로운 장면",
  "description": "씬 스토리 설명",
  "sceneId": null
}
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "scenarioSceneId": 1003,
    "sceneId": null,
    "order": 3,
    "title": "새로운 장면",
    "description": "씬 스토리 설명"
  }
}
```
※ `order`는 삽입 위치이며, 충돌 시 해당 위치 이후 항목을 +1 이동합니다. 저장 후 order는 1..N으로 정규화됩니다.

---

# 씬 스토리 순서 변경 (드래그 앤 드롭)
```
API /api/projects/{id}/scenario/scenes/order
메서드 PUT
보안 Bearer Token
상태 완료
설명 드래그 앤 드롭으로 변경된 씬 순서를 저장합니다.
```
※ `sceneId`가 매핑된 항목은 `/api/projects/{id}/scenes/order`에도 동일하게 반영됩니다.

#### 2. Request
```json
{
  "orderedScenarioSceneIds": [1002, 1001, 1003]
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| orderedScenarioSceneIds | List<Long> | 필수 | 정렬된 시나리오 씬 ID 목록 |

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "orderedScenarioSceneIds": [1002, 1001, 1003]
  }
}
```

---

# 씬 스토리 단건 수정
```
API /api/projects/{id}/scenario/scenes/{scenarioSceneId}
메서드 PATCH
보안 Bearer Token
상태 완료
설명 특정 시나리오 씬 항목의 제목/스토리를 단건 수정합니다.
```

#### 2. Request
```json
{
  "title": "수정된 씬 제목",
  "description": "수정된 씬 스토리",
  "order": 2
}
```
※ sceneId 매핑 변경은 전체 저장 API에서 처리합니다.

# AI 시나리오 생성 (Legacy, W3 제외)
```
API /api/ai/scenario
메서드 POST
보안 Bearer Token
상태 완료
설명 입력된 키워드와 장르를 바탕으로 시나리오, 줄거리, 씬 구성을 생성합니다.
```
※ W3 범위에서는 사용하지 않습니다. W3는 `/api/projects/{projectId}/scenario/*` 엔드포인트를 사용합니다.
※ 프로젝트에 저장/재조회가 필요하면 2.6.1 프로젝트 시나리오 API를 사용합니다.

#### 2. Request
```json
{
  "genre": "SF",
  "mood": "HOPEFUL",
  "keywords": "화성, 생존, 가족",
  "sceneCount": 5,
  "plot": "optional plot summary",
  // 고급 옵션 (선택)
  "characterHints": "외로운 우주인, 반항하는 로봇",
  "backgroundHints": "화성 기지, 붉은 사막",
  "referenceStyle": "인터스텔라 느낌"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| genre | String | 필수 | 장르 |
| mood | String | 필수 | 분위기/톤 |
| keywords | String | 선택 | 키워드 문자열 (콤마 구분) |
| sceneCount | Integer | 필수 | 생성할 씬 개수 (3~7개) |
| plot | String | 선택 | 초기 전체 줄거리 (입력 시 반영) |
| characterHints | String | 선택 | 메인 캐릭터 힌트 |
| backgroundHints | String | 선택 | 배경 힌트 |
| referenceStyle | String | 선택 | 참고할 작품/스타일 |

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "scenarioTitle": "화성의 희망",
    "plot": "화성에 고립되었지만...",
    "scenes": [
      { "order": 1, "title": "조난", "description": "..." },
      { "order": 2, "title": "교신 시도", "description": "..." }
    ]
  }
}
```

---

# 프롬프트 생성 (LLM)
```
API /api/ai/prompts/generate
메서드 POST
보안 Bearer Token
상태 완료
설명 사용자의 간단한 입력을 AI 이미지/영상 생성용 상세 프롬프트로 변환합니다.
```

#### 2. Request
```json
{
  "nodeType": "MASTER",
  "sceneOneLine": "화성 기지의 식당",
  "style": "실사",
  "timeOfDay": "아침",
  "mood": "편안",
  "objects": ["우주복", "테이블"]
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| nodeType | String | 필수 | 노드 타입 (MASTER, GRID, SHOT, VIDEO) |
| sceneOneLine | String | 필수 | 씬 한줄 설명 |
| style | String | 필수 | 스타일 라벨(자유 텍스트, 한글/영문 가능) |
| timeOfDay | String | 필수 | 시간대 라벨(자유 텍스트, 한글/영문 가능) |
| mood | String | 필수 | 분위기 라벨(자유 텍스트, 한글/영문 가능) |
| objects | List<String> | 선택 | 등장 오브젝트 텍스트 목록 |

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "prompt": "Cinematic wide shot of a futuristic Mars base cafeteria..."
  }
}
```

---

# 프롬프트 개선 (LLM)
```
API /api/ai/prompts/improve
메서드 POST
보안 Bearer Token
상태 완료
설명 기존 프롬프트에 사용자의 개선 요청(Instruction)을 반영하여 더 나은 프롬프트를 생성합니다.
```
※ instruction 미입력/빈 문자열이면 서버 기본 개선 지침을 적용합니다.

#### 2. Request
```json
{
  "nodeType": "MASTER",
  "prompt": "existing prompt...",
  "instruction": "Make it more cinematic and darker"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| nodeType | String | 필수 | 노드 타입 |
| prompt | String | 필수 | 기존 프롬프트 |
| instruction | String | 선택 | 개선 요청 사항 (미입력 시 기본 지침 적용) |

---

# AI 작업 상태 조회
```
API /api/ai/jobs/{jobId}
메서드 GET
보안 Bearer Token
상태 완료
설명 비동기 AI 작업의 현재 상태를 조회합니다. (Fallback용)
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "jobId": 12345,
    "type": "IMAGE_GENERATION",
    "status": "RUNNING",
    "progress": null,
    "target": { "type": "NODE", "id": 301 },
    "resultUrl": null,
    "error": null,
    "createdAt": "2026-01-19T10:11:12",
    "finishedAt": null
  }
}
```

#### 응답 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| jobId | Long | 필수 | Job ID |
| type | String | 필수 | `IMAGE_GENERATION` \| `VIDEO_GENERATION` \| `SCENE_MERGE` \| `PROJECT_MERGE` |
| status | String | 필수 | `PENDING` \| `RUNNING` \| `SUCCEEDED` \| `FAILED` |
| progress | Integer | 선택 | 진행률(%). MVP는 null |
| target | Object | 필수 | 요청 대상 (입력 기준) |
| target.type | String | 필수 | `NODE` \| `SCENE` \| `PROJECT` |
| target.id | Long | 필수 | 대상 ID |
| resultUrl | String | 선택 | 성공 시 결과 파일 URL |
| error | Object | 선택 | 실패 시 오류 |
| error.code | String | 선택 | 도메인 에러 코드 |
| error.message | String | 선택 | 오류 메시지 |
| createdAt | String | 선택 | 생성 시각 |
| finishedAt | String | 선택 | 완료 시각 |

> `target`은 요청 기준 대상입니다. (예: 노드 생성은 NODE, 프로젝트 병합은 PROJECT)

---

# AI 작업 재큐잉 (W4~W5)
```
API /api/ai/jobs/{jobId}/requeue
메서드 POST
보안 Bearer Token
상태 완료
설명 실패 또는 대기 상태의 Job을 같은 jobId로 다시 실행 요청합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "jobId": 12345,
    "type": "IMAGE_GENERATION",
    "status": "PENDING",
    "progress": null,
    "target": { "type": "NODE", "id": 301 },
    "resultUrl": null,
    "error": null,
    "createdAt": "2026-01-19T10:11:12",
    "finishedAt": null
  }
}
```

> W3 범위에서는 `requeue`/`regenerate`를 사용하지 않습니다. (W4~W5 확장)

---

### 2.7 타임라인 & 병합 API (W3)
정의:
- `videoNodeId`: 씬 내 확정된 VIDEO 노드 ID (씬 타임라인)
- `sceneVideoId`: 씬 병합 결과로 생성된 씬 영상 ID (프로젝트 타임라인)
`sceneVideoId` 흐름:
1) VIDEO 노드 확정: `/api/nodes/{id}/confirm`
2) 씬 타임라인 정렬: `PUT /api/scenes/{id}/timeline`
3) 씬 병합 요청: `POST /api/scenes/{id}/merge` -> `jobId`/`status` 반환
4) 완료 이벤트 수신: 프로젝트 이벤트 WS (job.done)
5) 조회: `GET /api/projects/{id}/timeline`에서 `sceneVideoId` 확인

# 씬 타임라인 조회
```
API /api/scenes/{id}/timeline
메서드 GET
보안 Bearer Token
상태 완료
설명 씬 내 확정된 VIDEO 노드(클립) 목록을 순서대로 반환합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "items": [
      {
        "videoNodeId": 401,
        "sceneId": 201,
        "thumbnailUrl": "https://...",
        "duration": 5,
        "order": 1
      }
    ],
    "totalDuration": 5
  }
}
```

---

# 씬 타임라인 순서 변경
```
API /api/scenes/{id}/timeline
메서드 PUT
보안 Bearer Token
상태 완료
설명 씬 내 클립 순서를 변경합니다.
```

#### 2. Request
```json
{
  "orderedVideoNodeIds": [401, 405, 403]
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| orderedVideoNodeIds | List<Long> | 필수 | 정렬된 영상 노드 ID 목록 |

---

# 프로젝트 타임라인 조회
```
API /api/projects/{id}/timeline
메서드 GET
보안 Bearer Token
상태 완료
설명 프로젝트 타임라인에 확정된 씬 영상 목록(`sceneVideoId`)을 순서대로 반환합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "items": [
      {
        "sceneVideoId": 401,
        "sceneId": 201,
        "sceneTitle": "화성 기지 아침",
        "thumbnailUrl": "https://...",
        "duration": 5,
        "order": 1
      },
      {
        "sceneVideoId": 405,
        "sceneId": 202,
        "sceneTitle": "탐사 출발",
        "thumbnailUrl": "https://...",
        "duration": 8,
        "order": 2
      }
    ],
    "totalDuration": 13
  }
}
```

---

# 씬 영상 병합 (Scene Merge)
```
API /api/scenes/{id}/merge
메서드 POST
보안 Bearer Token
상태 완료
설명 씬 내부의 확정된 영상들을 하나로 병합하여 씬 영상을 생성합니다.
```
※ 씬 타임라인 순서를 기준으로 병합합니다.
※ 완료 시 `sceneVideoId`가 생성되며 프로젝트 타임라인에 사용됩니다.

#### 2. Request
```json
{
  "includeMusic": false // P1: 배경음악 포함 여부
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| includeMusic | Boolean | 선택 | 배경음악 포함 여부 (기본: false, P1 기능) |

#### 3. Response
```json
{
  "code": "ACCEPTED",
  "data": {
    "jobId": 1001,
    "status": "PENDING"
  }
}
```

---

# 타임라인 순서 변경
```
API /api/projects/{id}/timeline
메서드 PUT
보안 Bearer Token
상태 완료
설명 프로젝트 타임라인 내 씬 영상 순서를 변경합니다.
```

#### 2. Request
```json
{
  "orderedSceneVideoIds": [401, 405, 403]
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| orderedSceneVideoIds | List<Long> | 필수 | 정렬된 씬 영상 ID 목록 |

---

# 최종 영상 병합 요청
```
API /api/projects/{id}/merge
메서드 POST
보안 Bearer Token
상태 완료
설명 타임라인의 영상들을 하나로 병합하여 최종 영화 파일을 생성합니다.
```
※ 프로젝트 타임라인 순서를 기준으로 병합합니다.

#### 2. Request
```json
{
  "includeMusic": false // P1: 배경음악 포함 여부
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| includeMusic | Boolean | 선택 | 배경음악 포함 여부 (기본: false, P1 기능) |

#### 3. Response
```json
{
  "code": "ACCEPTED",
  "data": {
    "jobId": 2001,
    "status": "PENDING"
  }
}
```

---

# 최종 영상 다운로드 (URL)
```
API /api/projects/{id}/export
메서드 GET
보안 Bearer Token
상태 완료
설명 최종 병합된 영상 파일의 다운로드 URL을 조회합니다.
```

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "downloadUrl": "https://s3.amazonaws.com/...",
    "expiresAt": "2026-01-15T18:00:00+09:00",
    "fileSize": 52428800,
    "format": "mp4",
    "duration": 60
  }
}
```

---

### 2.8 파일 API (P1)

# 파일 업로드 URL 발급
```
API /api/files/presign
메서드 POST
보안 Bearer Token
상태 완료
설명 S3에 파일을 직접 업로드하기 위한 Presigned URL을 발급받습니다.
```

#### 2. Request
```json
{
  "filename": "bgm.mp3",
  "contentType": "audio/mpeg"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| filename | String | 필수 | 파일명 (확장자 포함) |
| contentType | String | 필수 | MIME 타입 |

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "uploadUrl": "https://s3.amazonaws.com/presigned...",
    "fileKey": "uploads/user_1/bgm_1705312800.mp3",
    "expiresAt": "2026-01-15T17:00:00+09:00"
  }
}
```

---

# 파일 정보 조회
```
API /api/files/{id}
메서드 GET
보안 Bearer Token
상태 완료
```

---

# 파일 삭제
```
API /api/files/{id}
메서드 DELETE
보안 Bearer Token
상태 완료
```

---

# 음악 파일 업로드
```
API /api/music/upload
메서드 POST
보안 Bearer Token
상태 완료
설명 (P1) 음악 파일을 업로드합니다. (단, 대용량 파일은 Presigned URL 방식 권장)
```

#### 2. Request
`Content-Type: multipart/form-data`
- `file`: (Binary)
- `projectId`: (Long)

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| file | File | 필수 | 업로드할 음악 파일 |
| projectId | Long | 필수 | 프로젝트 ID |

#### 3. Response
```json
{
  "code": "SUCCESS",
  "data": {
    "musicId": 501,
    "url": "https://...",
    "duration": 120
  }
}
```

---

# 파일 업로드 완료 등록
```
API /api/files/complete
메서드 POST
보안 Bearer Token
상태 완료
설명 클라이언트가 S3 업로드를 마친 후, 서버에 파일 정보를 등록합니다. (Presigned URL 방식 사용 시 필수)
```

#### 2. Request
```json
{
  "fileKey": "uploads/123.mp3",
  "fileSize": 102400,
  "mimeType": "audio/mpeg"
}
```

#### 요청 필드 설명
| 필드 | 타입 | 필수 여부 | 설명 |
| --- | --- | --- | --- |
| fileKey | String | 필수 | S3 업로드 키 (경로) |
| fileSize | Long | 필수 | 파일 크기 (Byte) |
| mimeType | String | 필수 | 파일 MIME 타입 |

---

### 2.9 WebRTC 시그널링 (Raw WS, W5)

# WebRTC 시그널링
```
WS /ws/room/{roomId}
설명 화상통화 및 화면공유를 위한 P2P 연결 시그널링을 처리합니다. (W5 범위)
(보안: 연결 시 `Authorization` 헤더 또는 연결 직후 인증 메시지 전송 권장)
```
※ 인증 방식: 쿼리 파라미터 `token` 또는 `Authorization` 헤더 중 하나를 지원합니다.
※ roomId는 projectId와 동일하며, 프로젝트 멤버만 join 가능합니다.

#### 메시지 프로토콜 (JSON)
- `join`: 방 입장
- `leave`: 방 퇴장
- `offer`: WebRTC Offer 전달
- `answer`: WebRTC Answer 전달
- `candidate`: ICE Candidate 전달
- `chat`: 텍스트 채팅 메시지

---

### 2.10 프로젝트 이벤트 (STOMP, W3)

# 프로젝트 알림
```
STOMP /ws (SockJS)
Subscribe /topic/projects/{projectId}
설명 비동기 작업(AI 생성, 병합) 완료/실패 알림을 실시간으로 수신합니다.
```
※ 인증 방식: `Authorization: Bearer <JWT>` 헤더 사용
※ projectId 기준 프로젝트 멤버만 구독 가능합니다.

#### 이벤트 예시
```json
{
  "event": "job.done",
  "data": {
    "jobId": 123,
    "type": "VIDEO_GENERATION",
    "status": "SUCCEEDED",
    "target": { "type": "NODE", "id": 301 },
    "resultUrl": "/api/nodes/301/content"
  }
}
```

```json
{
  "event": "job.failed",
  "data": {
    "jobId": 2001,
    "type": "PROJECT_MERGE",
    "status": "FAILED",
    "target": { "type": "PROJECT", "id": 101 },
    "error": {
      "code": "MERGE_FAILED",
      "message": "병합에 실패했습니다. 잠시 후 다시 시도해주세요."
    }
  }
}
```


