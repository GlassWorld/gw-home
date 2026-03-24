# Todo: 프론트/백엔드 서버 분리 배포

> review 문서: `review/infra/01-split-server-deploy.md`

---

## 서버 구성 확정

| 역할 | 서버 IP | 포트 | 컨테이너 |
|------|---------|------|----------|
| Frontend | 158.179.168.138 | 80 | nginx + frontend |
| Backend  | 168.107.12.96   | 8080 | db (postgres:15) + backend |

**API 방식**: 프론트 nginx에서 `/api → 168.107.12.96:8080` 프록시 (NUXT_PUBLIC_API_BASE=/api 유지)

---

## GitHub Secrets 설정 가이드

> GitHub 저장소 → Settings → Secrets and variables → Actions → New repository secret

### 기존 secrets (유지 or 삭제)
```
OCI_HOST, OCI_USER, OCI_SSH_KEY, OCI_PORT, OCI_PROJECT_PATH
→ 더 이상 사용 안 함. 정리 권장 (삭제해도 무방)
```

### 프론트엔드 서버 secrets (신규 추가)
| Secret 이름 | 값 |
|------------|-----|
| `FRONT_HOST` | `158.179.168.138` |
| `FRONT_USER` | 서버 ssh 접속 유저명 (예: `ubuntu`, `opc`) |
| `FRONT_SSH_KEY` | 서버 SSH 개인키 전체 내용 (-----BEGIN ... -----END-----) |
| `FRONT_PORT` | `22` |
| `FRONT_APP_PATH` | `/home/{user}/app/frontend` |

### 백엔드 서버 secrets (신규 추가)
| Secret 이름 | 값 |
|------------|-----|
| `BACK_HOST` | `168.107.12.96` |
| `BACK_USER` | 서버 ssh 접속 유저명 |
| `BACK_SSH_KEY` | 서버 SSH 개인키 전체 내용 |
| `BACK_PORT` | `22` |
| `BACK_APP_PATH` | `/home/{user}/app/backend` |
| `DB_PASSWORD` | DB 비밀번호 (예: `gw1234`, 실제 운영값 사용 권장) |

### GHCR secrets (기존 유지)
| Secret 이름 | 설명 |
|------------|------|
| `GHCR_USERNAME` | GitHub 유저명 |
| `GHCR_TOKEN` | GitHub PAT (패키지 read 권한) |

---

## 서버 사전 준비 (수동 작업)

### 프론트엔드 서버 (158.179.168.138)
```bash
# SSH 접속 후
mkdir -p ~/app/frontend
# docker, docker-compose plugin 설치 확인
docker --version
docker compose version
```

### 백엔드 서버 (168.107.12.96)
```bash
# SSH 접속 후
mkdir -p ~/app/backend
# docker, docker-compose plugin 설치 확인
docker --version
docker compose version
```

---

## 작업 항목

- [x] 1. `deploy/frontend/nginx.conf` 생성 — `/api` → 백엔드 IP 프록시 포함
- [x] 2. `deploy/frontend/docker-compose.yml` 생성 — nginx + frontend
- [x] 3. `deploy/backend/docker-compose.yml` 생성 — db + backend
- [x] 4. `.github/workflows/deploy-front.yml` 수정 — FRONT_* secrets, 프론트 서버 SSH
- [x] 5. `.github/workflows/deploy-back.yml` 수정 — BACK_* secrets, 백 서버 SSH
- [x] 6. `.github/workflows/deploy-all.yml` 수정 — 두 서버 순차 배포

---

## 파일별 상세 계획

### 1. deploy/frontend/nginx.conf

```nginx
events {}

http {
  upstream frontend {
    server frontend:3000;
  }

  server {
    listen 80;
    server_name _;
    client_max_body_size 20M;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    # API → 백엔드 서버 직접 프록시
    location /api {
      proxy_pass http://168.107.12.96:8080;
      proxy_set_header Host              $host;
      proxy_set_header X-Real-IP         $remote_addr;
      proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Frontend SSR
    location / {
      proxy_pass http://frontend;
      proxy_http_version 1.1;
      proxy_set_header Host              $host;
      proxy_set_header X-Real-IP         $remote_addr;
      proxy_set_header Upgrade           $http_upgrade;
      proxy_set_header Connection        "upgrade";
    }
  }
}
```

### 2. deploy/frontend/docker-compose.yml

```yaml
services:
  frontend:
    image: ghcr.io/glassworld/gw-home-frontend:latest
    container_name: gw-fe-app
    restart: always
    environment:
      NUXT_PUBLIC_API_BASE: /api
    networks:
      - app-network

  nginx:
    image: nginx:latest
    container_name: gw-fe-nginx
    restart: always
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - frontend
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
```

### 3. deploy/backend/docker-compose.yml

```yaml
services:
  db:
    image: postgres:15
    container_name: gw-be-db
    restart: always
    environment:
      POSTGRES_DB: gw
      POSTGRES_USER: gw
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - db-data:/var/lib/postgresql/data
    networks:
      - app-network

  backend:
    image: ghcr.io/glassworld/gw-home-backend:latest
    container_name: gw-be-api
    restart: always
    depends_on:
      - db
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/gw
      SPRING_DATASOURCE_USERNAME: gw
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8080:8080"
    networks:
      - app-network

volumes:
  db-data:

networks:
  app-network:
    driver: bridge
```

### 4-6. GitHub Actions 워크플로우

deploy-front.yml SSH 스크립트:
```bash
mkdir -p ${{ secrets.FRONT_APP_PATH }}
cd ${{ secrets.FRONT_APP_PATH }}
# compose 파일과 nginx.conf를 서버로 전송 (scp step 추가)
echo "${{ secrets.GHCR_TOKEN }}" | docker login ghcr.io -u "${{ secrets.GHCR_USERNAME }}" --password-stdin
docker compose pull frontend
docker compose up -d
```

deploy-back.yml SSH 스크립트:
```bash
mkdir -p ${{ secrets.BACK_APP_PATH }}
cd ${{ secrets.BACK_APP_PATH }}
# compose 파일을 서버로 전송 (scp step 추가)
echo "${{ secrets.GHCR_TOKEN }}" | docker login ghcr.io -u "${{ secrets.GHCR_USERNAME }}" --password-stdin
docker compose pull backend
docker compose up -d
```

> compose 파일 전송: `appleboy/scp-action` 사용하거나 SSH script에서 `cat > docker-compose.yml << 'EOF'` heredoc 방식 사용
