# 08. FFmpeg 병합 Worker (MVP: concat, 무오디오)

> 목표: **기능적으로 동작**을 우선합니다.
>
> - 720p / H.264 / 오디오 없음 / 트랜지션 없음
> - “빠른 병합”이 목적이며, 고급 편집은 4주차 이후

---

## 1) 입력/출력 정의

### 입력 (타임라인 확정 클립 목록)
- projectId
- mergeJobId
- clips: 정렬된 clip 배열
  - clipId
  - sourceUrl: presigned URL 또는 내부 경로

### 출력
- outputPath (S3에 저장)
- resultUrl (presigned URL)

---

## 2) MVP 병합 방식: concat demuxer

- **전제:** 모든 입력 클립이 같은 코덱/해상도/프레임레이트일 때 가장 빠름
- 클립 스펙이 다르면: `transcode` 단계(느리지만 안전)를 선택

### (A) 빠른 concat (재인코딩 없음)

1) concat list 파일 생성

```
file 'clip1.mp4'
file 'clip2.mp4'
file 'clip3.mp4'
```

2) FFmpeg 실행

```bash
ffmpeg -f concat -safe 0 -i list.txt -c copy -an output.mp4
```

- `-an`: 오디오 제거

### (B) 안전한 concat (재인코딩)

```bash
ffmpeg -f concat -safe 0 -i list.txt \
  -vf "scale=1280:-2" -c:v libx264 -preset veryfast -crf 23 \
  -an -movflags +faststart output.mp4
```

---

## 3) Worker 동작 규칙 (Redis Streams)

- Streams: `merge.jobs`
- Consumer Group: `merge-workers`
- 메시지 필드(예시)

```json
{
  "jobId": "...",
  "projectId": "...",
  "timelineId": "...",
  "outputKey": "exports/.../final.mp4"
}
```

### 상태 전이
- pending → running → succeeded/failed
- 실패 시: `retryCount` 증가, 일정 횟수 초과하면 failed 고정

---

## 4) 멱등성(idempotency) 체크

- jobId 기준으로 결과가 이미 존재하면 **재실행하지 않고 성공 처리**
- 중복 실행 방지: DB에서 `running` 상태 락(Optimistic Lock 또는 `SELECT ... FOR UPDATE`)

---

## 5) 운영/성능 가이드(최소)

- 동시 merge 개수 제한: 1~2개부터 시작
- 파일 크기 제한(데모): 1분 이하 권장
- FFmpeg 로그는 `jobId`로 묶어서 저장
