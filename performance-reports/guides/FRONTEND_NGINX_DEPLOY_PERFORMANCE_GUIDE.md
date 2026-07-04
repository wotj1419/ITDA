# Frontend Nginx Deploy & Performance Measurement Guide

이 문서는 `itda-frontend`를 외부 Ubuntu 서버에 Nginx로 정적 배포하고, 프론트엔드 성능을 측정하기 위한 절차입니다.

목표는 처음 배포하는 사람도 아래 흐름대로 따라가며 랜딩 페이지와 인증 페이지의 성능을 측정할 수 있게 하는 것입니다.

## 0. 목표 범위

1차 목표는 프론트엔드 정적 배포입니다.

```txt
측정 대상: /, /auth
배포 대상: itda-frontend/dist
서빙 방식: Nginx static hosting
아직 제외: 백엔드, DB, Redis, S3, 로그인 이후 화면
```

이 범위만으로도 다음 항목을 측정할 수 있습니다.

```txt
Lighthouse Performance
FCP
LCP
TBT
CLS
초기 요청 수
총 전송량
JS/CSS/font/video 리소스 크기
Nginx gzip/cache-control 적용 여부
```

## 1. 준비물

외부에서 접속 가능한 Ubuntu 서버 1대가 필요합니다.

추천 환경:

```txt
Ubuntu 22.04 또는 24.04
Node.js 22+
npm
Nginx
Git
서버 IP
SSH 접속 권한
```

클라우드 보안 그룹 또는 방화벽에서 아래 포트를 열어둡니다.

```txt
22   SSH
80   HTTP
443  HTTPS, 추후 사용
```

처음 성능 측정은 도메인 없이 서버 IP로도 가능합니다.

## 2. 서버 접속

로컬 PC 터미널에서 서버에 접속합니다.

```bash
ssh ubuntu@서버IP
```

예시:

```bash
ssh ubuntu@123.123.123.123
```

서버 사용자명이 `ubuntu`가 아닐 수 있습니다. 클라우드 제공자에 따라 `ec2-user`, `root` 등을 사용할 수 있습니다.

## 3. 기본 패키지 설치

서버에서 실행합니다.

```bash
sudo apt update
sudo apt install -y nginx git curl
```

Nginx 상태를 확인합니다.

```bash
sudo systemctl status nginx
```

브라우저에서 아래 주소로 접속합니다.

```txt
http://서버IP
```

Nginx 기본 화면이 보이면 정상입니다.

## 4. Node.js 22 설치

프론트엔드를 서버에서 빌드하려면 Node.js가 필요합니다.

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
```

설치 확인:

```bash
node -v
npm -v
```

`node -v`가 `v22.x.x`로 나오면 됩니다.

## 5. 프로젝트 받기

서버에서 프로젝트를 받을 위치로 이동합니다.

```bash
cd ~
git clone <프로젝트_저장소_URL> ITDA
cd ITDA
```

예시:

```bash
git clone https://github.com/계정/저장소.git ITDA
cd ITDA
```

프로젝트 구조를 확인합니다.

```bash
ls
```

`itda-frontend` 폴더가 보여야 합니다.

## 6. 프론트엔드 빌드

프론트엔드 폴더로 이동합니다.

```bash
cd ~/ITDA/itda-frontend
```

의존성을 설치합니다.

```bash
npm ci
```

빌드합니다.

```bash
npm run build
```

성공하면 아래 폴더가 생성됩니다.

```txt
~/ITDA/itda-frontend/dist
```

이 `dist` 폴더가 실제 배포할 정적 파일입니다.

## 7. Nginx 배포 폴더 만들기

Nginx가 정적 파일을 읽을 폴더를 만듭니다.

```bash
sudo mkdir -p /var/www/itda
```

빌드 결과물을 복사합니다.

```bash
sudo cp -r ~/ITDA/itda-frontend/dist/. /var/www/itda/
```

복사 확인:

```bash
ls -la /var/www/itda
```

`index.html`, `assets` 등이 보여야 합니다.

## 8. Nginx 설정 만들기

Nginx 사이트 설정 파일을 생성합니다.

```bash
sudo nano /etc/nginx/sites-available/itda
```

아래 내용을 넣습니다.

도메인이 없다면 `server_name _;` 그대로 사용합니다.

```nginx
server {
    listen 80;
    server_name _;

    root /var/www/itda;
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;
    gzip_min_length 1024;

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }

    location ~* \.(png|jpg|jpeg|gif|svg|webp|ico|mp4|woff|woff2|ttf)$ {
        expires 30d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

설정 의미:

```txt
root /var/www/itda
  dist 파일을 서빙할 위치입니다.

location / { try_files ... /index.html; }
  Vue Router를 사용하는 SPA 라우팅을 지원합니다.

gzip on
  JS/CSS 같은 텍스트 리소스 전송량을 줄입니다.

location /assets/
  Vite가 해시 파일명으로 생성한 정적 assets에 장기 캐시를 적용합니다.
```

## 9. Nginx 설정 활성화

사이트 설정을 활성화합니다.

```bash
sudo ln -s /etc/nginx/sites-available/itda /etc/nginx/sites-enabled/itda
```

Nginx 설정 문법을 검사합니다.

```bash
sudo nginx -t
```

정상이라면 Nginx를 다시 로드합니다.

```bash
sudo systemctl reload nginx
```

기본 Nginx 화면이 계속 보이면 기본 사이트 설정을 비활성화합니다.

```bash
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

## 10. 접속 확인

브라우저에서 아래 주소를 확인합니다.

```txt
http://서버IP/
http://서버IP/auth
```

랜딩 페이지와 인증 페이지가 보이면 1차 배포 성공입니다.

## 11. 개선 전 성능 측정

배포 직후 측정값은 "개선 전" 기준값으로 저장합니다.

Chrome DevTools Lighthouse에서 측정합니다.

```txt
URL: http://서버IP/
Mode: Navigation
Device: Mobile, Desktop 각각 측정
Category: Performance
```

기록할 항목:

```txt
Lighthouse Performance
FCP
LCP
TBT
CLS
총 요청 수
총 전송량
JS 전송량
CSS 전송량
Font 전송량
Video 전송량
가장 큰 리소스
```

Chrome DevTools Network 탭에서 특히 확인할 항목:

```txt
scene-1.mp4가 초기 로딩 시점에 다운로드되는지
TTF 폰트들이 다운로드되는지
JS chunk 크기가 어느 정도인지
gzip이 적용되는지
cache-control 헤더가 적용되는지
```

이 프로젝트에서 우선 확인할 리소스:

```txt
/scene-1.mp4
/TmoneyRoundWindExtraBold.ttf
/TTTogether.ttf
/BMJUA_ttf.ttf
/assets/*.js
/assets/*.css
```

## 12. 측정 결과 정리 양식

아래 표 형태로 개선 전 값을 먼저 기록합니다.

```txt
항목                    개선 전      개선 후      변화
Lighthouse Performance  -            -            -
FCP                     -            -            -
LCP                     -            -            -
TBT                     -            -            -
CLS                     -            -            -
초기 요청 수             -            -            -
총 전송량                -            -            -
JS 전송량                -            -            -
CSS 전송량               -            -            -
Font 전송량              -            -            -
Video 전송량             -            -            -
가장 큰 리소스            -            -            -
```

## 13. 최적화 후 재배포

프론트엔드 최적화 작업을 한 뒤 다시 빌드합니다.

```bash
cd ~/ITDA/itda-frontend
npm run build
```

기존 배포 파일을 교체합니다.

```bash
sudo rm -rf /var/www/itda/*
sudo cp -r ~/ITDA/itda-frontend/dist/. /var/www/itda/
sudo systemctl reload nginx
```

다시 같은 조건으로 Lighthouse와 Network를 측정합니다.

성능 비교에서는 조건을 최대한 동일하게 유지합니다.

```txt
같은 URL
같은 브라우저
같은 Lighthouse 모드
같은 네트워크 조건
같은 페이지
같은 로그인 상태
```

## 14. 포트폴리오에 쓰기 좋은 측정 포인트

이 프로젝트에서는 다음 항목이 프론트엔드 성과로 정리하기 좋습니다.

```txt
1. 랜딩 페이지 LCP/FCP/Lighthouse 개선
2. hero video preload 전략 개선
3. 대용량 TTF 폰트 최적화
4. 초기 JS 번들 크기 및 route-level code splitting 확인
5. Nginx gzip/cache-control 적용 효과
6. /auth 등 주요 정적 라우트 초기 로딩 비교
```

포트폴리오 문장 예시:

```txt
실제 서비스와 동일한 Nginx 정적 배포 환경에서 Lighthouse와 Web Vitals를 측정하고,
랜딩 페이지의 대용량 hero video와 폰트 리소스 병목을 분석해 초기 전송량과 LCP를 개선했습니다.
```

## 15. 다음 단계

프론트 정적 배포가 완료된 뒤, 로그인 이후 화면까지 측정하려면 백엔드 포함 배포가 필요합니다.

추가로 필요한 구성:

```txt
Spring Boot API
MySQL
Redis
S3 또는 S3 호환 스토리지
GCP service account
JWT secret
/api 프록시
/ws WebSocket 프록시
테스트 계정
```

처음 배포 단계에서는 백엔드까지 한 번에 붙이지 말고, 먼저 프론트 정적 배포와 랜딩 페이지 성능 측정을 완료하는 것을 권장합니다.
