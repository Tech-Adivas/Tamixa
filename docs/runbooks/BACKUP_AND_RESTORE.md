# PostgreSQL backup and restore (runbook)

**Goal:** Meet Phase 1 enterprise bar: **backups exist** and **restore is practiced** before you depend on them in an incident.

Applies to any PostgreSQL deployment (RDS, Cloud SQL, self-managed, Docker volume).

---

## 1. Backup

### Managed cloud (preferred)

- **AWS RDS / Aurora:** Enable automated backups; set retention (e.g. 7–35 days). Optionally use snapshot before major migrations.
- **GCP Cloud SQL:** Automated backups + point-in-time recovery if enabled in tier.

### Logical dump (portable)

From a machine that can reach the DB:

```bash
# Replace vars. Never commit credentials.
export PGHOST=... PGPORT=5432 PGUSER=... PGPASSWORD=... PGDATABASE=...
pg_dump -Fc -f "tamixa-$(date -u +%Y%m%d-%H%M%S).dump" "$PGDATABASE"
```

Store the file in **encrypted** object storage (S3/GCS) with **versioning** and **least-privilege** IAM.

---

## 2. Tested restore (quarterly minimum)

1. **Provision** a throwaway database (new RDS instance, or local Docker Postgres).
2. **Restore** the latest backup (or a recent snapshot) into that instance.
3. **Run Flyway** against the restored DB only if you are validating schema compatibility; for a full snapshot restore, Flyway history is already in the dump—follow your restore path (dump vs snapshot) per vendor docs.
4. **Smoke:** Point a **non-production** backend config at the restored DB; hit `GET /actuator/health/readiness` and one read-only API if safe.
5. **Document** date, who ran it, and any issues in your ops log.

If restore has never been tested, **you do not have backups**—you have hope.

---

## 3. Tamixa-specific notes

- Migrations live under `backend/src/main/resources/db/migration` (Flyway).
- Production should use `SPRING_PROFILES_ACTIVE=prod` and **not** the `dev` profile or dev seed endpoints ([DEPLOYMENT_CHECKLIST.md](../DEPLOYMENT_CHECKLIST.md)).
- After restore in a new environment, verify **JWT**, **Stripe webhook secrets**, and **S3 bucket** config match that environment (do not point prod keys at a clone by mistake).

---

## 4. Related docs

- [DEPLOYMENT_CHECKLIST.md](../DEPLOYMENT_CHECKLIST.md)
- [PRODUCTION_UPGRADE.md](../PRODUCTION_UPGRADE.md)
- [INCIDENT_RESPONSE.md](../INCIDENT_RESPONSE.md)
