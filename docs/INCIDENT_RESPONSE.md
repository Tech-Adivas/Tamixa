# Incident Response — One-Pager

**Quick reference for security and privacy incidents.**  
Full plan: [COMPLIANCE.md — Section 10 (Incident Response Plan) and Section 11 (Breach Notification)](./COMPLIANCE.md#10-incident-response-plan).

## Roles (assign and keep updated)

| Role | Responsibility | Contact |
|------|-----------------|---------|
| **Incident lead** | Coordinate response; declare severity; internal/external comms | _[Assign: e.g. Tech Lead or Security Owner]_ |
| **Tech lead / Engineer** | Containment; evidence preservation; remediation | _[Assign]_ |
| **Compliance / DPO** | Regulatory notification; breach register; internal record | _[Assign: e.g. compliance@company.com]_ |
| **Executive** | Escalation; external communication if needed | _[Assign]_ |

## Severity

- **P1 – Critical:** Data breach (PII/child data); prolonged outage → immediate containment + compliance.
- **P2 – High:** Security event with potential data impact → involve compliance.
- **P3 – Medium:** Security event, low/no data impact.
- **P4 – Low:** Minor issue; no data impact.

## Steps (high level)

1. **Detect & report** → Assign incident lead (use [Incident channel / on-call]).
2. **Triage** → P1/P2: involve Compliance/DPO; preserve logs and evidence.
3. **Contain** → Isolate systems; revoke access; block bad actors.
4. **Eradicate** → Patch; rotate credentials; fix config.
5. **Recover** → Restore; verify; monitor.
6. **Post-incident** → Root cause; timeline; breach notification if required (see COMPLIANCE.md Section 11).

## Breach notification (if PII/child data affected)

- **GDPR:** Notify supervisory authority within **72 hours**; notify data subjects without undue delay if high risk.
- **Child data:** Notify parents/guardians and regulators as required (see COMPLIANCE.md Section 11).

## Where to look

- **Full Incident Response Plan:** [COMPLIANCE.md § 10](./COMPLIANCE.md#10-incident-response-plan)
- **Breach procedure & timelines:** [COMPLIANCE.md § 11](./COMPLIANCE.md#11-breach-notification-procedure)
- **Audit logs / preservation:** Backend `StructuredAuditLogger`; ensure log retention and access for investigations.

---

*Update the contact table above when roles or channels change. Keep this doc linked from onboarding and runbooks.*
