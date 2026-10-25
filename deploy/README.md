# EscapeLink test deployment

This deployment runs independently from other Docker Compose projects on the host.
The public entry point is a static nginx container. It serves the exported Next.js
frontend and proxies `/api` and `/ws` to the private Spring Boot container.

1. Copy `.env.deploy.example` to `.env.deploy` and replace both passwords.
2. Set `ESCAPELINK_PUBLIC_ORIGIN` to the exact URL used by mobile browsers.
3. Start the stack:

   ```bash
   docker compose --env-file .env.deploy -f compose.deploy.yml up -d --build
   ```

4. Check the public page and backend health:

   ```bash
   curl -fsS http://localhost:3002/admin/login
   curl -fsS http://localhost:8082/actuator/health
   ```

The test profile uses HTTP, so both session cookies are explicitly configured as
non-secure. Set `ESCAPELINK_COOKIE_SECURE=true` when the public origin is moved
behind HTTPS. Do not leave the HTTP test deployment exposed longer than needed;
credentials and session cookies are not encrypted in transit.

Bootstrap credentials are create-once values. Changing the admin password in
`.env.deploy` does not rotate an existing account. Likewise, PostgreSQL only
uses its password variable when initializing a fresh volume; rotate persisted
credentials in the database before changing that value on an existing stack.

## CI/CD (GitHub Actions)

`.github/workflows/ci-cd.yml` runs on every push and pull request:

1. **Backend tests** (`./gradlew test`, Testcontainers PostgreSQL) and **frontend lint / typecheck / build**.
2. On `main` only: **ARM64 images** are built natively and pushed to GHCR
   (`ghcr.io/<owner>/escapelink-backend|frontend`, tagged with the commit SHA and `latest`).
3. On `main` only: **deploy** over SSH to the server (GitHub environment `production`).

The server side is a restricted SSH key: `~/.ssh/authorized_keys` forces it to run
`~/escapelink/deploy.sh` (`restrict` removes shell, pty and forwarding). The script reads a
short-lived registry token, the commit SHA and `compose.deploy.yml` from stdin, then runs
`docker compose pull` + `up -d --wait`. It is deliberately not updated by CI; copy
`deploy/deploy.sh` to the server by hand when it changes. `.env.deploy` stays on the server.

One-time setup (environment `production` secrets):

| Secret | Value |
|---|---|
| `DEPLOY_SSH_KEY` | private half of the deploy key |
| `DEPLOY_KNOWN_HOSTS` | `stw.iptime.org ssh-ed25519 <server host public key>` |
| `DEPLOY_HOST` / `DEPLOY_USER` | server host name / login user |

Server authorized_keys line:

```
command="/home/stw/escapelink/deploy.sh",restrict ssh-ed25519 AAAA... escapelink-github-deploy
```

Rollback: run `ESCAPELINK_TAG=<older sha> docker compose --env-file .env.deploy -f compose.deploy.yml up -d`
on the server (older images stay in the local cache until pruned).
