# GitHub Actions 배포 자동화 검토

## 작업 개요

GitHub Actions를 이용해 OCI Ubuntu 서버에 Docker Compose 기반 자동 배포 파이프라인 구성.

## 대상 파일

```
.github/workflows/
├── deploy-front.yml   # frontend 단독 배포
├── deploy-back.yml    # backend 단독 배포
└── deploy-all.yml     # 공통/전체 변경 시 배포
```

---

## 배포 방식

- 배포 대상 서버: OCI Ubuntu (SSH 접속)
- 실행 명령: 서버에서 `git pull` → `docker compose up -d --build {service}`
- 인증: GitHub Secrets에 SSH key 등록

---

## 트리거 전략

| 워크플로우 | 트리거 조건 |
|-----------|------------|
| `deploy-front.yml` | `gw-home-ui/**` 변경 시 |
| `deploy-back.yml` | `gw-home-api/**`, `gw-home-share/**`, `gw-home-infra-db/**` 변경 시 |
| `deploy-all.yml` | `deploy/**`, `docker-compose.yml`, `nginx/**` 등 공통 변경 시 |

> `deploy-all.yml`은 인프라/공통 파일 변경 시 전체 재빌드·재배포 실행.

---

## 필요한 GitHub Secrets

| Secret 키 | 설명 |
|-----------|------|
| `OCI_HOST` | 서버 IP |
| `OCI_USER` | SSH 접속 유저 (예: `ubuntu`) |
| `OCI_SSH_KEY` | SSH private key |
| `OCI_PROJECT_PATH` | 서버 내 프로젝트 경로 (예: `/home/ubuntu/gw-home`) |

---

## 배포 흐름

```
push to main
    └─ 변경 경로 감지
         ├─ gw-home-ui/**          → deploy-front.yml
         │    └─ SSH → git pull → docker compose up -d --build frontend
         │
         ├─ gw-home-api/** 등      → deploy-back.yml
         │    └─ SSH → git pull → docker compose up -d --build backend
         │
         └─ deploy/**, docker-compose.yml 등 → deploy-all.yml
              └─ SSH → git pull → docker compose up -d --build
```

---

## 리스크 분석

| 리스크 | 내용 | 대응 |
|--------|------|------|
| 빌드 중 다운타임 | `--build` 동안 컨테이너 재시작 발생 | 현재 규모에서는 허용 수준 |
| 동시 배포 충돌 | front/back 동시 push 시 두 워크플로우 동시 실행 | `concurrency` 그룹 설정으로 직렬화 |
| SSH key 노출 | Secrets 미설정 시 접속 불가 | GitHub Secrets 필수 등록 안내 포함 |
| git pull 충돌 | 서버에서 로컬 변경이 있을 경우 | 서버는 read-only 운영 원칙 (직접 수정 금지) |

---

## 대안 검토

| 방안 | 장점 | 단점 |
|------|------|------|
| SSH + git pull + compose (채택) | 단순, 추가 인프라 불필요 | 서버에 git/docker 필요 |
| Docker Hub push + pull | 이미지 버전 관리 가능 | Docker Hub 계정·요금 필요 |
| GitHub Container Registry | 무료, GitHub 통합 | 설정 복잡도 증가 |
