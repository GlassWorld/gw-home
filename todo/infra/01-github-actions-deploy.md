# GitHub Actions 배포 자동화

> 검토 문서: `review/infra/01-github-actions-deploy.md`

## 작업 범위

신규 파일 3개 생성. 기존 파일 수정 없음.

```
.github/workflows/
├── deploy-front.yml
├── deploy-back.yml
└── deploy-all.yml
```

---

## 작업 목록

- [ ] `.github/workflows/deploy-back.yml` 생성
  - 트리거: `gw-home-api/**`, `gw-home-share/**`, `gw-home-infra-db/**`
  - 실행: SSH → git pull → `docker compose up -d --build backend`
  - concurrency 그룹: `deploy`

- [ ] `.github/workflows/deploy-front.yml` 생성
  - 트리거: `gw-home-ui/**`
  - 실행: SSH → git pull → `docker compose up -d --build frontend`
  - concurrency 그룹: `deploy`

- [ ] `.github/workflows/deploy-all.yml` 생성
  - 트리거: `deploy/**`, `docker-compose.yml`, `.github/workflows/**`
  - 실행: SSH → git pull → `docker compose up -d --build`
  - concurrency 그룹: `deploy`

---

## 사용 Secrets

| 키 | 값 |
|----|-----|
| `OCI_HOST` | 144.24.79.156 |
| `OCI_USER` | ubuntu |
| `OCI_SSH_KEY` | RSA private key |
| `OCI_PORT` | 22 |
| `OCI_PROJECT_PATH` | /home/ubuntu/gw-home |
