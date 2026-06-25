# Running

JDK:
- Java 17
- Default path: `C:\Program Files\Java\jdk-17`

Backend with project-local MySQL (recommended on this machine):
```powershell
.\start-backend-mysql.ps1
```

This starts a project-local MySQL instance on `127.0.0.1:3307`, creates database `cloudbrain_medical`, creates user `cloudbrain / cloudbrain_dev`, and starts the backend with the `mysql` profile.

If port `8088` is already occupied:
```powershell
.\start-backend-mysql.ps1 -Port 8081
```

Backend with Docker MySQL:
```powershell
docker compose -f docker-compose.mysql.yml up -d

$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/cloudbrain_medical?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="cloudbrain"
$env:DB_PASSWORD="cloudbrain_dev"
.\mvnw.cmd -pl backend spring-boot:run
```

Flyway runs the database migrations automatically when the backend starts.

Backend with default in-memory H2:
```powershell
.\mvnw.cmd -pl backend test
.\mvnw.cmd -pl backend spring-boot:run
```

Frontend:
```powershell
cd frontend
npm install
npm run dev
```

Production build:
```powershell
.\mvnw.cmd -pl backend package

cd frontend
npm install
npm run build
```

Jar startup example:
```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_URL="jdbc:mysql://127.0.0.1:3307/cloudbrain_medical?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="cloudbrain"
$env:DB_PASSWORD="cloudbrain_dev"
java -jar backend\target\cloud-brain-medical-backend-0.1.0-SNAPSHOT.jar
```

URLs:
- Backend: `http://localhost:8088`
- Health check: `http://localhost:8088/api/health`
- Frontend: `http://localhost:5173`

Realtime and dashboard checks:
- WebSocket notification endpoint: `/ws/notifications?token=<jwt>`
- SSE session flow: `POST /api/ai-stream-sessions`, then `GET /api/ai-stream-sessions/{id}/events?token=<streamToken>`
- Dashboard APIs: `/api/dashboard/overview`, `/api/dashboard/trends`, `/api/dashboard/ai-usage`, `/api/dashboard/prescription-review-rate`, `/api/dashboard/risk-distribution`, `/api/dashboard/triage-accuracy`
- Nginx reference config: `deploy/nginx.conf`
- Deployment acceptance checklist: `docs/部署验收.md`
