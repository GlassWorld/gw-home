# Review: 프론트/백엔드 서버 분리 배포

## 작업 개요

현재 단일 서버 기반의 GitHub Actions 배포를 **서버 분리 구조**로 전환한다.

| 역할 | 서버 IP | 컨테이너 |
|------|---------|----------|
| Frontend | 168.107.58.105 | `nginx` + `frontend` |
| Backend  | 168.107.12.96   | `db` (PostgreSQL) + `backend` |

---

## 현재 상태 분석

### 현재 구조 (문제점)
- `docker-compose.yml` 루트: `backend` + `frontend` 단일 파일 (nginx 없음, db 없음)
- GitHub Actions: 모든 워크플로우가 동일한 `OCI_HOST` 단일 서버에 SSH
- `deploy/nginx/default.conf`: 동일 서버 내 `/api` → backend, `/` → frontend 프록시 구성 (현재 미사용)

### 필요한 변경
1. 서버별 `docker-compose.yml` 분리
2. 프론트 전용 nginx.conf (backend 프록시 없음)
3. GitHub Actions 워크플로우 SSH 타겟 분리

---

## 변경 파일 목록

### 신규 생성
| 파일 | 내용 |
|------|------|
| `deploy/frontend/docker-compose.yml` | `frontend` + `nginx` (포트 80) |
| `deploy/frontend/nginx.conf` | `/ → frontend:3000` 프록시만 포함 |
| `deploy/backend/docker-compose.yml` | `db` (postgres:15) + `backend` (포트 8080) |

### 수정
| 파일 | 변경 내용 |
|------|-----------|
| `.github/workflows/deploy-front.yml` | SSH 타겟 → `FRONT_*` secrets, 프론트 서버에만 배포 |
| `.github/workflows/deploy-back.yml` | SSH 타겟 → `BACK_*` secrets, 백 서버에만 배포 |
| `.github/workflows/deploy-all.yml` | 두 서버에 순차 SSH 배포 |

---

## GitHub Secrets 변경

### 기존 (삭제 또는 유지)
```
OCI_HOST, OCI_USER, OCI_SSH_KEY, OCI_PORT, OCI_PROJECT_PATH
```

### 추가 필요
```
FRONT_HOST       = 168.107.58.105
FRONT_USER       = (서버 사용자명)
FRONT_SSH_KEY    = (SSH 개인키)
FRONT_PORT       = 22
FRONT_APP_PATH   = ~/app/frontend

BACK_HOST        = 168.107.12.96
BACK_USER        = (서버 사용자명)
BACK_SSH_KEY     = (SSH 개인키)
BACK_PORT        = 22
BACK_APP_PATH    = ~/app/backend
```

---

## ⚠️ 핵심 확인 사항

### 1. NUXT_PUBLIC_API_BASE 처리 방법

서버 분리 후 Nuxt(SSR)에서 백엔드 API를 호출하는 방식이 달라진다.

**옵션 A**: 백엔드 IP 직접 지정
```
NUXT_PUBLIC_API_BASE=http://168.107.12.96:8080
```
- 브라우저와 SSR 서버 모두 백엔드 IP:8080 직접 호출
- 백엔드 서버 방화벽에서 8080 포트 오픈 필요

**옵션 B**: 프론트 nginx에서 `/api` 프록시 추가 (백엔드 IP로)
```nginx
location /api {
  proxy_pass http://168.107.12.96:8080;
}
```
- `NUXT_PUBLIC_API_BASE=/api` 유지 가능
- 프론트 서버가 백엔드 호출을 중계 (네트워크 홉 추가)

**→ 어떤 방식을 원하시나요?**

### 2. 배포 방식

현재: 서버에서 `git pull` 후 compose 실행
제안: 서버에 git 불필요 → CI에서 compose 파일을 `scp`로 전송 후 실행

**옵션 A**: 서버에서 `git pull` 유지 (서버에 git + repo clone 필요)
**옵션 B**: CI가 compose 파일 전송 → `docker compose pull && up` (더 심플)

### 3. 백엔드 환경변수

현재 docker-compose.yml에 DB 정보가 없음. 분리 배포 시 필요:
```
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/gw
SPRING_DATASOURCE_USERNAME=gw
SPRING_DATASOURCE_PASSWORD=UQTiDCsYmD5oHYDL  ← GitHub Secret으로 관리 권장
```

---

## 리스크

| 항목 | 리스크 | 대응 |
|------|--------|------|
| 기존 OCI 서버 컨테이너 중단 | 기존 서버 배포 중단 | 신규 서버 배포 확인 후 기존 중단 |
| DB 데이터 없음 | 백엔드 서버 postgres 첫 실행 | volume 마운트 확인 |
| CORS | 프론트/백 서버 다른 오리진 | Spring CORS 설정 확인 필요 |
| 방화벽 | 각 서버 포트 오픈 여부 | 80 (프론트), 8080 (백) |

---

## 관련 문서

- 기존 워크플로우: `.github/workflows/deploy-front.yml`, `deploy-back.yml`, `deploy-all.yml`
- nginx 설정: `deploy/nginx/default.conf`
- Dockerfile: `deploy/frontend/Dockerfile`, `deploy/backend/Dockerfile`
