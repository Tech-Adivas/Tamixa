# Tamixa — Compliance & Cybersecurity Documentation

**Document type:** Policy & Compliance Framework  
**Product:** Tamixa (AI storytelling app for children)  
**Version:** 1.0  
**Last updated:** February 2026  
**Classification:** Internal — Investor & Regulator Ready

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Compliance Status & Auditor Feedback](#2-compliance-status--auditor-feedback)
3. [Regulatory Compliance Mapping](#3-regulatory-compliance-mapping)
4. [Data Classification Policy](#4-data-classification-policy)
5. [Data Flow Diagram](#5-data-flow-diagram)
6. [Data Retention Policy](#6-data-retention-policy)
7. [Parental Consent Workflow](#7-parental-consent-workflow)
8. [Child Data Separation Model](#8-child-data-separation-model)
9. [Encryption Standards](#9-encryption-standards)
10. [Incident Response Plan](#10-incident-response-plan)
11. [Breach Notification Procedure](#11-breach-notification-procedure)
12. [Vendor Risk Assessment (OpenAI)](#12-vendor-risk-assessment-openai)
13. [Data Deletion Workflow](#13-data-deletion-workflow)
14. [Audit Logging Policy](#14-audit-logging-policy)
15. [Access Control Policy](#15-access-control-policy)
16. [Secure Development Lifecycle (SDLC)](#16-secure-development-lifecycle-sdlc)
17. [Implementation Checklist for Engineers](#17-implementation-checklist-for-engineers)

---

## 1. Executive Summary

Tamixa is an AI-powered storytelling application for children (target age 1–12). Parents register, create child profiles, and request age-appropriate stories; the system uses OpenAI for generation and optional voice/TTS features. This document establishes compliance with:

| Regulation | Scope | Tamixa applicability |
|------------|--------|----------------------|
| **India DPDP Act 2023** | Processing of digital personal data of Indian residents | Full — consent, purpose limitation, data principal rights, children’s data |
| **COPPA (US)** | Online services directed at children &lt;13 | Full — parental consent, minimal collection, no conditioning on data |
| **GDPR (EU)** | Processing of personal data in EU / EEA | Full — lawful basis, children’s data (Art. 8), rights, DPIAs where needed |
| **SOC 2** | Security, availability, confidentiality (trust principles) | Readiness — controls mapped to criteria |

All child-related data is treated as **sensitive**; access is restricted to the custodial parent account and to systems necessary for service delivery.

---

## 2. Compliance Status & Auditor Feedback

This section maps senior/compliance feedback to current controls and remediation. Use it for audit evidence and prioritised improvements.

### 2.1 GDPR

| Feedback | Status | Evidence / remediation |
|----------|--------|-------------------------|
| Explicit consent for Terms/Privacy | ✅ Done | Registration and passwordless verify require checkboxes; stored with account (AuthApi, ConsentController). |
| Right to delete (logout clears tokens) | ✅ Done | Logout clears tokens; full account/child deletion in Data Deletion Workflow (Section 13).  |
| No explicit data export mechanism | ⚠️ Addressed | **Data export is implemented:** `POST /v1/data-export/request` and `GET /v1/data-export` (DataExportController); async job produces JSON (DataExportPayload) and stores in S3; mobile/web Settings expose "Request data export". Ensure Privacy Policy and in-app copy explicitly mention "You can request a copy of your data" with link to Settings. |
| No data retention policy documented | ⚠️ Addressed | **Retention is documented in this document:** Section 6 (Data Retention Policy)  defines periods by data type (e.g. stories 24 months, audit 90 days / 12 months). Publish a short retention summary in the Privacy Policy and link here for full policy. |

### 2.2 COPPA

| Feedback | Status | Evidence / remediation |
|----------|--------|-------------------------|
| Age verification exists but no parental consent flow for under 13 | ⚠️ Partially addressed | **No child accounts:** Only parents register; child profiles (age 1–12) are created by parents. Parental consent is required before creating a child profile (`childProfileConsent` in ChildService; ConsentService records `child_profile_consent`). **Gap:** Add explicit parental attestation at registration (e.g. "I am the parent/guardian and am at least 18 years old" checkbox) and document as verifiable parental consent method. |
| No explicit data minimization policy | ⚠️ Addressed | **Data minimization** is implied in Data Classification (Section 4) and child entity fields (name, DoB, language, optional voice). Add explicit **Data Minimization Policy** in Privacy Policy and here: we collect only what is necessary for story generation, voice, and account; no behavioural advertising; no sale of child data. See Section 7.3 (Child data for AI) below. |
| No clear policy on how child data is used for AI generation | ⚠️ Addressed | **Child data for AI:** We send to the **configured LLM provider** (OpenAI or Google Gemini per `AI_LLM_PROVIDER`): age, language, theme, and first name for personalisation. No parent email, no child ID; vendor retention and subprocessors follow the active provider’s terms/DPA. Document this in Privacy Policy under "How we use your child's information" and in Section 12 (Vendor Risk Assessment). |

### 2.3 PCI-DSS

| Feedback | Status | Evidence / remediation |
|----------|--------|-------------------------|
| Stripe webhook signature verification | ✅ Done | WebhookController verifies `Stripe-Signature` via WebhookSignatureVerifier; invalid signature returns 400. |
| No evidence of PCI compliance audit | ⚠️ Document | We do not store card data; Stripe is PCI-DSS compliant and we use Stripe.js/Checkout (SAQ A or similar). Document in a short **PCI scope statement:** "Card data is handled exclusively by Stripe; we do not store, process, or transmit cardholder data. Our scope is limited to redirect/embed and webhook receipt; we rely on Stripe's AOC for card environment." |
| Webhook payload encryption optional (should be required) | ⚠️ Addressed | **Remediation:** When Stripe webhooks are enabled in production, webhook payloads stored at rest must be encrypted. Set `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` (32-byte Base64 AES key) in production. Backend startup validation can require this when Stripe is enabled and profile is prod (see Implementation Checklist). |

### 2.4 Security Best Practices

| Feedback | Status | Evidence / remediation |
|----------|--------|-------------------------|
| OWASP Top 10 mitigations (injection, broken auth, XSS) | ✅ Done | Parameterised queries; JWT auth; no raw user input in SQL; output encoding / CSP where applicable (security rules). |
| No evidence of penetration testing | ⚠️ Recommend | **Recommendation:** Conduct annual penetration test (or after major changes); retain report for auditors. Document in SDLC (Section 16): "Pen test recommended annually; findings tracked and remediated." |
| No security monitoring/alerting (Sentry, Datadog, etc.) | ⚠️ Recommend | **Recommendation:** Integrate error tracking (e.g. Sentry) and optional APM/metrics (e.g. Datadog) for production. Alert on: auth failures, 5xx spikes, webhook signature failures, critical exceptions. Document in Audit Logging / runbooks. |
| No incident response plan documented | ⚠️ Addressed | **Incident response is documented:** Section 10 (Incident Response Plan) and Section 11 (Breach Notification). A one-pager is available at **`docs/INCIDENT_RESPONSE.md`** with roles, severity, steps, and link to this section; assign incident lead and Compliance/DPO contacts and keep the doc updated. |

### 2.5 Data minimisation (explicit policy)

We collect and retain only what is necessary to:

- Provide the service (account, child profiles, stories, optional voice).
- Comply with law and enforce terms.

We do **not** use child or parent data for behavioural advertising, and we do **not** sell personal data. Child data is used for age-appropriate story generation and optional voice cloning only; see Vendor Risk Assessment (Section 12) for OpenAI usage.

### 2.6 Child data use for AI generation (policy statement)

- **Sent to OpenAI:** Only the minimum needed for story generation: child's first name (for personalisation), age (from DoB), language, and story theme. We do **not** send parent email, child ID, or any other identifiers.
- **Retention at OpenAI:** Per our DPA and OpenAI terms, we do not use OpenAI to store Tamixa data; request/response are transient.
- **No training on Tamixa data:** We use the API under terms that do not use customer data for model training (confirm in DPA).
- **Moderation:** Story content may be sent to OpenAI Moderation API for safety; same minimal retention applies.

---

## 3. Regulatory Compliance Mapping

### 3.1 India DPDP Act 2023

| DPDP requirement | Tamixa control | Evidence / implementation |
|------------------|----------------|----------------------------|
| Consent (S. 6) | Explicit parental consent before child profile creation & story generation | Parental consent workflow; consent recorded at registration & per child |
| Purpose limitation (S. 8) | Data collected only for stated purposes (story generation, voice, account) | Data classification & retention; no secondary use without consent |
| Data principal rights: access, correction, erasure, grievance (S. 11–14) | Self-service and support workflows; data deletion workflow | Data deletion workflow; grievance mechanism in Privacy Policy |
| Children’s data (S. 9) | Verifiable parental consent; no processing of child data for tracking/targeting | Child data separation; no behavioural ads; consent workflow |
| Data fiduciary obligations (S. 8–10) | Security, accuracy, retention limits, breach notification | Encryption, retention policy, incident/breach procedures |
| Consent Manager (optional) | Documented for future integration | Policy placeholder |

### 3.2 COPPA (US — Children’s Online Privacy Protection)

| COPPA requirement | Tamixa control | Evidence / implementation |
|-------------------|----------------|----------------------------|
| Verifiable parental consent before collection (16 CFR 312.5) | Parent registers; creates child profile only after consent | Parental consent workflow; no child-directed sign-up |
| Notice (what is collected, how used) (16 CFR 312.4) | Privacy Policy & in-app notice at consent | Parental consent workflow; Privacy Policy |
| Minimal collection (16 CFR 312.6) | Only name, DoB, language, optional voice; age 1–12 | Data classification; child entity fields |
| No conditioning on unnecessary data | Service not conditioned on extra PII | Policy; product design |
| Parental access & deletion (16 CFR 312.6) | Parent sees all child data; can delete child/profile | Child data separation; data deletion workflow |
| Confidentiality & security (16 CFR 312.8) | Encryption, access control, vendor agreements | Encryption standards; access control; OpenAI DPA |

### 3.3 GDPR (EU)

| GDPR requirement | Tamixa control | Evidence / implementation |
|------------------|----------------|----------------------------|
| Lawful basis (Art. 6) | Consent (parent) for child data; contract for account | Consent workflow; terms of service |
| Children’s data (Art. 8) | Parental consent for &lt;16 (or lower per MS); no child account | Child data separation; age 1–12 via parent only |
| Transparency (Art. 12–14) | Privacy notice; consent at collection | Parental consent workflow; Privacy Policy |
| Rights: access, rectification, erasure, portability, object, restrict (Art. 15–21) | Data deletion workflow; export and correction via support | Data deletion workflow; access control |
| Security (Art. 32) | Encryption, access control, audit, vendor safeguards | Encryption; access control; audit logging; vendor assessment |
| DPA with processors (Art. 28) | OpenAI DPA; standard clauses where data leaves EEA | Vendor risk assessment |
| Breach notification (Art. 33–34) | 72h to SA; high-risk to data subjects | Breach notification procedure |
| DPIAs (Art. 35) | For high-risk processing (children, AI) | SDLC; DPIA trigger in checklist |

### 3.4 SOC 2 Trust Service Criteria (readiness)

| SOC 2 criterion | Tamixa control | Evidence / implementation |
|-----------------|----------------|----------------------------|
| CC6.1 – Logical access | JWT + role-based access; parent-only access to child data | Access control policy; SecurityConfig; ChildService |
| CC6.2 – Prior to issuance | Access granted per role; removal on offboarding | Access control policy |
| CC6.6 – Encryption | AES-256 at rest (voice); TLS in transit | Encryption standards; AesEncryptionService |
| CC7.1 – Detection of security events | Audit logs (login, story, voice, subscription) | Audit logging policy; StructuredAuditLogger |
| CC7.2 – Monitoring | Log aggregation; alerting on anomalies | Audit logging policy; SIEM integration |
| CC8.1 – Change management | SDLC; code review; secure deployment | SDLC section |
| A1.2 – Availability | Health checks; redundancy per deployment | application.yml actuators |

---

## 4. Data Classification Policy

### 4.1 Classification levels

| Level | Description | Examples in Tamixa |
|-------|-------------|-------------------|
| **Restricted (child PII)** | Data that identifies a child; highest protection | Child name, date of birth, language preference, voice profile, story content associated with child |
| **Confidential (parent PII)** | Data that identifies the parent/account holder | Parent email, password hash, JWT tokens, subscription details |
| **Internal** | Operational data not PII | Story themes, cache keys (hashed), metrics, trace IDs |
| **Public** | No sensitivity | Public API docs, marketing content |

### 4.2 Data inventory (Tamixa)

| Data asset | Classification | Location | Owner |
|------------|----------------|----------|--------|
| Parent email | Confidential | PostgreSQL `parent` | Backend |
| Parent password hash | Confidential | PostgreSQL `parent` | Backend |
| Child name, DoB, language | Restricted | PostgreSQL `child` | Backend |
| Voice profile (audio) | Restricted | Object storage / DB (encrypted) | Backend |
| Story content (with child name/theme) | Restricted | PostgreSQL `story`, Redis cache | Backend |
| Story content sent to OpenAI | Restricted | Transient (API request/response) | OpenAI (processor) |
| JWT access/refresh tokens | Confidential | Client / memory | Client + Backend |
| Audit logs (email masked) | Internal | Log stream / SIEM | Backend |
| Prometheus metrics | Internal | Metrics backend | Backend |

### 4.3 Handling rules

- **Restricted:** Encrypt at rest (AES-256 where applicable); access only for parent-owner and authorised support; never used for marketing or behavioural advertising.
- **Confidential:** Encrypt in transit (TLS 1.3); hashed passwords; access per need-to-know.
- **Internal:** No PII in logs; trace IDs for correlation only; retention per audit policy.

---

## 5. Data Flow Diagram

Text-based data flow for Tamixa (parent and child data).

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              TAMIXA DATA FLOW (TEXT)                               │
└─────────────────────────────────────────────────────────────────────────────────┘

 [Parent/Guardian]                    [Tamixa Web / Mobile]
       │                                        │
       │  Register (email, password)             │
       │  Consent (Terms + Privacy + Child)      │
       ├───────────────────────────────────────►│
       │                                        │
       │                        ┌───────────────┴───────────────┐
       │                        │  API Gateway / Backend (TLS)   │
       │                        │  JWT validation, rate limit     │
       │                        └───────────────┬───────────────┘
       │                                        │
       │  Create child (name, DoB, language)     │
       ├───────────────────────────────────────►│
       │                                        │
       │                        ┌───────────────┴───────────────┐
       │                        │  Auth + ChildService           │
       │                        │  parentId → child linked       │
       │                        └───────────────┬───────────────┘
       │                                        │
       │                        ┌───────────────▼───────────────┐
       │                        │  PostgreSQL                   │
       │                        │  parent, child, story,        │
       │                        │  voice_profile (encrypted)    │
       │                        └───────────────┬───────────────┘
       │                                        │
       │  Generate story (theme, childId, age)   │
       ├───────────────────────────────────────►│
       │                        │               │
       │                        │  StoryService │
       │                        │  buildPrompt(age, language, theme, childName)
       │                        │               │
       │                        │  ┌────────────▼────────────┐
       │                        │  │ OpenAI API (HTTPS)      │
       │                        │  │ Request: prompt only    │
       │                        │  │ Response: story text    │
       │                        │  │ (no storage by policy)   │
       │                        │  └────────────┬────────────┘
       │                        │               │
       │                        │  Content moderation (OpenAI)
       │                        │  Save story → DB; optional Redis cache
       │                        │  Publish event → Kafka (audio pipeline)
       │                        │               │
       │  Story / audio          │               │
       │◄───────────────────────────────────────┤
       │                                        │
       │  Voice upload (optional)                │
       ├───────────────────────────────────────►│
       │                        │  AES-256-GCM encrypt → store
       │                        │  Audit: voice_upload
       │                        │
       │  All access to child data               │
       │  filtered by parentId (ChildService)    │
       │  PreAuthorize("hasRole('PARENT')")      │
       └────────────────────────────────────────┘

 EXTERNAL PROCESSORS:
 - OpenAI: story generation + moderation; prompt/response transient per DPA.
 - (Optional) TTS/audio service: receives story content via Kafka.
 - (Optional) Cloud storage: encrypted voice assets.
```

### 5.1 Flow summary table

| Flow | Data | Direction | Protection |
|------|------|-----------|------------|
| Registration | Email, password | Client → Backend | TLS; password hashed (BCrypt) |
| Child profile create | Name, DoB, language | Client → Backend | TLS; stored in DB; parentId binding |
| Story generate | Theme, age, language, child name | Backend → OpenAI | TLS; no persistent storage at OpenAI per DPA |
| Story save | Content, parentId, childId, theme | Backend → PostgreSQL / Redis | TLS; DB at rest; cache TTL |
| Voice upload | Audio binary | Client → Backend | TLS; AES-256-GCM at rest |
| Login / refresh | Email, tokens | Client ↔ Backend | TLS; JWT; audit log (email masked) |

---

## 6. Data Retention Policy

### 6.1 Retention by data type

| Data type | Retention period | Legal / operational basis | Deletion method |
|-----------|------------------|----------------------------|------------------|
| Parent account (email, hash) | Until account deletion requested | Contract; consent | Data deletion workflow |
| Child profile | Until parent deletes child or account | Consent; COPPA/DPDP/GDPR | Data deletion workflow |
| Story content | 24 months or until parent/child deletion (whichever first) | Service delivery; limit exposure | Data deletion workflow; story purge job |
| Voice profile (encrypted) | Until parent removes or account deletion | Service delivery | Data deletion workflow |
| Redis story cache | TTL 24 hours (configurable) | Performance | Automatic expiry (STORY_CACHE_TTL_HOURS) |
| Audit logs | 90 days (operations); 12 months (compliance copy) | SOC 2; incident investigation | Log retention; archive then purge |
| Kafka events | Per topic retention (e.g. 7 days) | Pipeline processing | Broker retention |
| JWT | Access: 15 min; Refresh: 7 days | Security | Expiry only; no long-term storage |

### 6.2 Retention principles

- **Minimisation:** Retain only as long as necessary for the stated purpose or legal obligation.
- **Child data:** No indefinite retention; deletion on request or when child exceeds age cap (e.g. 12) per policy.
- **Documentation:** Retention periods documented in Privacy Policy and in this policy.

---

## 7. Parental Consent Workflow

### 7.1 Consent points

| Step | Action | Consent captured | Record |
|------|--------|-------------------|--------|
| 1 | Parent visits app / website | — | — |
| 2 | Parent reads Privacy Policy & Terms | Link to policies | Required before next step |
| 3 | Parent clicks “Register” / “Sign up” | Agreement to Terms + Privacy Policy | Timestamp; IP (optional); consent version |
| 4 | Parent submits email + password | Account creation consent | Stored with account |
| 5 | Before creating first child profile | “I am the parent/guardian and consent to collect [list] for this child” | Timestamp; consent version; optional consent_id |
| 6 | Before first story generation for child | Confirmation that use of AI (OpenAI) for stories is accepted | Can be combined with step 5 |
| 7 | Optional: voice recording | Explicit consent for voice collection and processing | Timestamp; scope (e.g. TTS only) |

### 7.2 Implementation requirements (engineers)

- **Registration:** Present Terms + Privacy Policy with checkbox/link; do not allow submit without acceptance; store consent timestamp and policy version.
- **Per-child consent:** Before first child profile save, show short consent (data collected: name, DoB, language; purpose: stories; third party: OpenAI); store consent per child or per account with child scope.
- **Withdrawal:** Parent can withdraw consent by deleting child profile or account; data deletion workflow applies.
- **Auditability:** Consent events should be logged (e.g. consent_given: child_id, type, version) without logging full PII in plaintext.

### 7.3 Compliance mapping

| Regulation | Consent requirement | Tamixa implementation |
|------------|---------------------|------------------------|
| DPDP S. 6 | Consent for processing | Parent consent at registration and per child |
| COPPA | Verifiable parental consent | Parent account = verifiable; consent before child data |
| GDPR Art. 6, 8 | Consent / parental responsibility | Explicit consent; no child account |

---

## 8. Child Data Separation Model

### 8.1 Design principles

- **No child accounts:** Children do not log in; no child credentials or direct child-facing accounts.
- **Parent as sole data controller interface:** All child data is created, read, updated, and deleted only in the context of the parent account.
- **Strict binding:** Every child record has a non-null `parentId`; every story has `parentId` and optional `childId`; access control enforces parent ownership.

### 8.2 Access control (current implementation)

| Resource | Who can access | How enforced |
|----------|----------------|--------------|
| Child CRUD | Parent that owns the child | `ChildService.findByIdForParent(childId, parentEmail)`; `child.parentId == parent.id` |
| Story list/detail | Parent that owns the story | Story repository filtered by `parentId` from JWT |
| Voice profile | Parent that owns the profile | Voice API scoped by parent (from JWT) |
| Auth endpoints | Anonymous (register/login/refresh) | No child data in auth payload |

### 8.3 Data model (logical)

- **Parent:** id, email, passwordHash, role, createdAt.
- **Child:** id, parentId, name, dateOfBirth, languagePreference, createdAt.  
  All access via parent; no API key or token issued to child.
- **Story:** id, parentId, childId (optional), content, theme, language, age, childName, wordCount, readingTimeMinutes, createdAt.  
  Always tied to parent; optionally to child for display only.
- **VoiceProfile:** parent/child association per product design; encrypted at rest.

### 8.4 Compliance mapping

| Regulation | Requirement | Tamixa implementation |
|------------|-------------|----------------------|
| COPPA | No collection from child without parental consent | No child account; parent creates profile after consent |
| DPDP S. 9 | Children’s data with consent | Child data only under parent; consent workflow |
| GDPR Art. 8 | Parental consent for children | Same; parent-only access |

---

## 9. Encryption Standards

### 9.1 Standards (policy)

| Context | Standard | Implementation note |
|---------|----------|---------------------|
| **Data in transit** | TLS 1.3 (prefer) or TLS 1.2 minimum | All client–backend and backend–LLM/TTS providers (e.g. OpenAI, Google); disable TLS 1.0/1.1 |
| **Data at rest – voice / sensitive binaries** | AES-256-GCM | 256-bit key; 12-byte IV; 128-bit auth tag (current: AesEncryptionService) |
| **Passwords** | BCrypt (industry standard) | No reversible storage; Spring Security BCryptPasswordEncoder |
| **Secrets** | Not stored in code or logs | Environment variables / secret manager (e.g. VOICE_ENCRYPTION_KEY, JWT_SECRET, OPENAI_API_KEY, GEMINI_API_KEY) |

### 9.2 Key management

- **Voice encryption key (VOICE_ENCRYPTION_KEY):** 32 bytes (256 bits), Base64-encoded; stored in environment or secret manager; rotated per key rotation policy (e.g. annually or on compromise).
- **JWT secret (JWT_SECRET):** Minimum 256 bits for HS256; same as above; rotation invalidates existing tokens (acceptable with short expiry).
- **TLS:** Certificate and private key managed by infrastructure (e.g. load balancer / ingress); minimum TLS 1.2.

### 9.3 Compliance mapping

| Regulation / framework | Encryption expectation | Tamixa |
|------------------------|------------------------|--------|
| DPDP / GDPR | Appropriate technical measures | AES-256 at rest for voice; TLS in transit |
| SOC 2 CC6.6 | Encryption of sensitive data | Implemented as above |
| COPPA | Security of children’s data | Same standards applied to all child data |

---

## 10. Incident Response Plan

### 10.1 Roles

| Role | Responsibility |
|------|----------------|
| **Incident lead** | Coordinate response; declare severity; communicate |
| **Tech lead / Engineer** | Containment; evidence preservation; remediation |
| **Compliance / DPO** | Regulatory notification; internal record |
| **Executive** | Escalation; external communication (if needed) |

### 10.2 Severity levels

| Level | Description | Example |
|-------|-------------|--------|
| **P1 – Critical** | Data breach (e.g. PII/child data exfiltrated or exposed); prolonged outage | DB leak; unauthorised access to child records |
| **P2 – High** | Security event with potential data impact; partial outage | Suspected compromise; failed access control |
| **P3 – Medium** | Security event with low/no data impact | Brute-force attempts; malware in non-sensitive system |
| **P4 – Low** | Minor security issue; no data impact | Misconfiguration; non-sensitive log exposure |

### 10.3 Phases

1. **Detection & reporting:** Monitoring, alerts, or user report → assign incident lead.
2. **Triage:** Classify severity (P1–P4); involve compliance if P1/P2 or potential breach.
3. **Containment:** Isolate affected systems; revoke access; block malicious actors; preserve logs and snapshots.
4. **Eradication:** Remove cause (patch, credential rotation, config fix).
5. **Recovery:** Restore service; verify integrity; monitor.
6. **Post-incident:** Root cause; timeline; actions; update runbooks; breach notification if applicable (see Section 10).

### 10.4 Contact and tools

- Internal: [Incident channel / on-call]; Compliance/DPO: [Contact].
- Preserve: audit logs, access logs, DB backups (if relevant), network logs.

---

## 11. Breach Notification Procedure

### 11.1 When to notify

- **Personal data breach:** Unlawful or accidental access, loss, destruction, or alteration of personal data (GDPR Art. 4(12); DPDP; COPPA expectations).
- **Child data:** Any breach involving child PII triggers high priority and, where required, parental/regulator notification.

### 11.2 Timelines

| Jurisdiction | Authority | Data subjects |
|-------------|-----------|----------------|
| **GDPR** | Supervisory authority: **72 hours** (Art. 33) | Without undue delay if high risk (Art. 34) |
| **India DPDP** | As per DPDP rules (when issued) | Data principal without undue delay |
| **US (state laws)** | Per state (e.g. 30–90 days) | Affected residents |
| **COPPA** | FTC if material; state AGs as required | Parents of affected children |

### 11.3 Steps

1. **Confirm breach** (unauthorised access / disclosure / loss of PII).
2. **Document:** What data, how many subjects, likely impact, containment actions.
3. **Notify authority** within 72 hours (GDPR) where applicable; comply with local rules (India, US states).
4. **Notify data subjects** (and parents for child data) when risk to rights and freedoms is likely; include: nature of breach, contact for questions, recommended steps.
5. **Notify processors** (e.g. OpenAI) if breach involves their processing; coordinate per DPA.
6. **Internal record:** Breach register (date, description, scope, notifications, lessons learned).

---

## 12. Vendor Risk Assessment (OpenAI Usage)

### 12.1 Role of OpenAI in Tamixa

- **Story generation:** Prompt (age, language, theme, child name) and received story text.
- **Content moderation:** Story text sent to Moderation API.
- **Data flow:** Request/response over HTTPS; no persistent storage of Tamixa data by OpenAI per DPA/terms.

### 12.2 Risk assessment

| Risk | Level | Mitigation |
|------|--------|------------|
| Child name / age in prompt | Medium | Minimal prompt; no other PII; DPA and no-training terms (if applicable) |
| Story content processed by OpenAI | Medium | DPA; clarify no training on customer data; short retention per OpenAI policy |
| Outage / dependency | Medium | Retry (current); future: fallback or degrade gracefully |
| Change in OpenAI policy | Low | Annual review; contract/DPA review |
| Sub-processors | Low | Rely on OpenAI’s sub-processor list and DPA |

### 12.3 Requirements

- **DPA (Data Processing Agreement):** In place with OpenAI; includes confidentiality, security, sub-processors, deletion, and audit support.
- **No training on Tamixa data:** Use API under terms that do not use customer data for model training (confirm in DPA/terms).
- **Minimisation:** Send only prompt and story text necessary for generation and moderation; no parent email or child ID in API payload.
- **Review:** Annual vendor review; reassess on new product features (e.g. new OpenAI endpoints).

### 12.4 Compliance mapping

| Regulation | Processor requirement | Tamixa / OpenAI |
|------------|------------------------|----------------|
| GDPR Art. 28 | DPA; processor obligations | DPA in place; processor only; no storage of Tamixa PII long-term |
| DPDP | Processor obligations (as per Act/rules) | DPA and contractual safeguards |
| COPPA | Third-party disclosure in notice; contract | Privacy Policy disclosure; DPA |
| SOC 2 | Vendor risk management | This assessment; annual review |

---

## 13. Data Deletion Workflow

### 13.1 Triggers

- Parent requests deletion of a **child profile**.
- Parent requests **full account deletion**.
- Legal obligation (e.g. court order).
- Retention period reached (e.g. story purge after 24 months).

### 13.2 Steps (full account deletion)

| Step | Action | Owner | Status |
|------|--------|--------|--------|
| 1 | Authenticate parent and verify identity | Backend / support | ✅ Implemented |
| 2 | Revoke all sessions (invalidate refresh tokens; optional blocklist) | Backend | ✅ **IMPLEMENTED** (JWT revocation via Redis) |
| 3 | Delete or anonymise all children for this parent | Backend | ✅ Implemented |
| 4 | Delete or anonymise all stories for this parent | Backend | ✅ Implemented |
| 5 | Delete or anonymise voice profiles | Backend / storage | ✅ Implemented |
| 6 | Delete parent account (email, password hash) | Backend | ✅ Implemented |
| 7 | Purge Redis cache for related keys | Backend | ✅ Implemented |
| 8 | Queue or run purge of Kafka-related state (if any) | Backend | N/A |
| 9 | Log deletion event (e.g. account_id, date; no PII) | Audit | ✅ Implemented |
| 10 | Confirm to parent (e.g. email) | Support / automated | ⚠️ Manual |

**JWT Revocation Implementation (Step 2):**
- `AccountDeletionService` calls `tokenRevocationPort.revokeAllForUser(email)` before deletion
- All access and refresh tokens issued before deletion timestamp are invalidated
- Redis stores user-level revocation timestamp with 30-day TTL
- See `/JWT_REVOCATION_IMPLEMENTATION_SUMMARY.md` for full details

### 13.3 Child-only deletion

- Delete child record; delete or anonymise stories where `childId` = this child; remove voice profile if linked to child; audit log.
- Parent can request export before deletion (GDPR/DPDP).

### 13.4 Implementation checklist (engineers)

- [ ] API or internal job: delete by parentId (children → stories → voice → parent).
- [ ] Transactions where needed to keep referential integrity.
- [ ] No hard-delete of audit logs required for compliance (retain per audit policy); optionally anonymise if needed by law.
- [ ] Document timeline (e.g. “within 30 days”) in Privacy Policy and internal SLA.

---

## 14. Audit Logging Policy

### 14.1 Events to log (current and required)

| Event | Current | Fields (minimal; avoid PII) | Retention |
|-------|---------|-----------------------------|-----------|
| Login attempt | Yes (StructuredAuditLogger) | Masked email, success, traceId, timestamp | 90 days ops; 12 months compliance |
| Story generation | Yes | storyId, parentId, theme, traceId, timestamp | Same |
| Voice upload | Yes | parentId, voiceProfileId, traceId, timestamp | Same |
| Subscription change | Yes | parentId, action, details, traceId, timestamp | Same |
| Child create/delete | Recommended | childId, parentId, action, timestamp | Same |
| Consent | Recommended | consent_type, version, timestamp (no child name in log) | Same |
| Data export / deletion | Required | action, scope (account/child), timestamp | Same |
| Admin / privileged access | Required when implemented | who, what, when | Same |

### 14.2 Log protection

- Logs must not contain passwords, full JWT, or unencrypted child/parent PII (child name in log minimised or hashed if needed for forensics).
- Current: email masked in login (e.g. first 2 chars + *** + domain).
- Store logs in a restricted, append-only or access-controlled system; integrity (e.g. hashing) recommended for compliance copy.

### 14.3 Compliance mapping

| Regulation / framework | Audit requirement | Tamixa |
|------------------------|-------------------|--------|
| SOC 2 CC7.1 | Security event logging | Login, story, voice, subscription; extend to consent and deletion |
| GDPR / DPDP | Demonstrate compliance | Audit trail for access and processing |
| COPPA | Security of data | Access and processing logged |

---

## 15. Access Control Policy

### 15.1 Principles

- **Least privilege:** Each role gets only the access needed.
- **Role-based:** PARENT role for all parent users; no child role (no child login).
- **Resource-level:** Access to child/story/voice scoped by parentId (enforced in service layer).

### 15.2 Roles

| Role | Scope | Tamixa implementation |
|------|--------|-----------------------|
| **Anonymous** | Register, login, refresh only | SecurityConfig: permitAll for auth endpoints |
| **PARENT** | Own profile; own children; own stories; own voice | PreAuthorize("hasRole('PARENT')"); ChildService/StoryService filter by parent |
| **Admin** (future) | Support / compliance only; no routine access to PII | Separate role; MFA; audit all access |

### 15.3 Authentication

- **Mechanism:** JWT (access + refresh); stateless (SessionCreationPolicy.STATELESS).
- **Access token:** Short-lived (e.g. 15 min per application.yml).
- **Refresh token:** Longer-lived (e.g. 7 days); stored securely on client; revocable on logout or account deletion.
- **Secrets:** JWT secret from environment; minimum 256 bits.

### 15.4 Authorisation

- All authenticated requests carry JWT; `parentEmail` (or equivalent) from token used to resolve `parentId` and filter all child/story/voice access.
- No endpoint returns another parent’s child or story; ChildService throws ChildAccessDeniedException when parentId does not match.

### 15.5 Compliance mapping

| Regulation / framework | Access control expectation | Tamixa |
|------------------------|----------------------------|--------|
| SOC 2 CC6.1, CC6.2 | Logical access; authorisation | JWT + PARENT role; parentId scoping |
| GDPR / DPDP | Access only as necessary | Same |
| COPPA | Confidentiality and security | Parent-only access to child data |

---

## 16. Secure Development Lifecycle (SDLC)

### 16.1 Phases and security activities

| Phase | Activity |
|-------|-----------|
| **Requirements** | Identify PII/child data; privacy requirements; DPIA trigger (children, AI, new processing) |
| **Design** | Threat model for new features; data flow; no child data in logs/analytics by default |
| **Implementation** | Secure coding (parameterised queries; no secrets in code); use EncryptionPort, AuditLogPort |
| **Review** | Code review (security + privacy); dependency scan (e.g. OSS) |
| **Test** | Security tests (authz, injection); tests for access control (e.g. ChildAccessDenied) |
| **Deploy** | Secrets from vault/env; TLS; no debug in production |
| **Operate** | Monitor logs; patch; incident response |

### 16.2 DPIA trigger (GDPR Art. 35)

- Processing of **children’s data** (systematic).
- **AI/automated** decision-making or profiling that significantly affects users.
- **Large-scale** sensitive data or special categories.

For Tamixa: child data + AI story generation = DPIA recommended for EU users; document purpose, necessity, risks, and mitigations (consent, minimal data, no profiling, vendor DPA).

### 16.3 Security checklist per release

- [ ] No new PII in logs.
- [ ] New endpoints authorised (role + resource scope).
- [ ] New third-party calls documented; DPA/terms checked.
- [ ] Dependency scan clean or accepted risks documented.
- [ ] Secrets not in code or config in repo.

---

## 17. Implementation Checklist for Engineers

Use this checklist to align implementation with this compliance framework.

### 17.1 Data & consent

- [ ] **Consent at registration:** Terms + Privacy Policy acceptance stored with timestamp and version.
- [ ] **Parental attestation (COPPA):** At registration, explicit checkbox: "I am the parent or guardian and am at least 18 years old" (or equivalent); store with consent records for audit.
- [ ] **Consent per child:** Consent step before first child profile save; stored with account or child.
- [ ] **Data classification:** All new fields/APIs classified (Restricted / Confidential / Internal); no Restricted in logs.
- [ ] **Retention:** New data types have defined retention and purge (job or on deletion).

### 17.2 Child data & access

- [ ] **Child data separation:** All child access via parent; `parentId` check in service layer.
- [ ] **No child login:** No child-facing auth or token.
- [ ] **Age validation:** Child age 1–12 enforced (current: ChildService.validateAge).

### 17.3 Encryption & transport

- [ ] **TLS 1.2+** for all external connections (client, LLM/TTS providers).
- [ ] **Voice at rest:** AES-256-GCM; key from env/secret manager (current: AesEncryptionService).
- [ ] **Passwords:** BCrypt only; no reversible storage.
- [ ] **Secrets:** JWT_SECRET, VOICE_ENCRYPTION_KEY, OPENAI_API_KEY and/or GEMINI_API_KEY (per configuration) from environment only.
- [ ] **Webhook payload encryption (PCI/production):** When Stripe (or payment webhooks) is enabled in production, set `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` (32-byte Base64 AES key) so webhook payloads stored at rest are encrypted. Backend may enforce this at startup when `app.subscription.require-webhook-payload-encryption=true` and Stripe is enabled.

### 17.4 Audit & logging

- [ ] **Login attempts:** Logged with masked email (current).
- [ ] **Story generation, voice upload, subscription:** Logged (current).
- [ ] **Child create/delete:** Logged (childId, parentId, action; no child name if possible).
- [ ] **Consent events:** Logged (type, version, timestamp).
- [ ] **Data deletion/export:** Logged (scope, timestamp).
- [ ] **No PII in logs:** No full email, child name, or story content in plaintext.

### 17.5 Access control

- [ ] **JWT only** for authenticated APIs; stateless.
- [ ] **PARENT role** enforced on all child/story/voice endpoints (current).
- [ ] **Resource scope:** Every child/story/voice access filtered by parentId (current in ChildService).

### 17.6 Deletion & rights

- [ ] **Account deletion:** Removes parent, children, stories, voice; revokes sessions; audit log.
- [ ] **Child deletion:** Removes child and associated stories/voice; audit log.
- [ ] **Export (GDPR/DPDP):** Mechanism to export parent + child data (manual or API).

### 17.7 Vendor (OpenAI)

- [ ] **DPA** signed; no-training terms confirmed for API usage.
- [ ] **Prompt minimal:** Only age, language, theme, child name; no parent email or IDs in prompt.
- [ ] **Vendor review:** Annual review of OpenAI and other processors.

### 17.8 Incident & breach

- [ ] **Incident runbook** accessible; roles (incident lead, compliance) assigned.
- [ ] **Breach procedure** (72h authority; subject notification) documented and known to compliance.
- [ ] **Log preservation** possible for incident investigation (retention, access).

### 17.9 Story AI safety (production refactor)

The Story AI module has been refactored for production-grade safety and reliability:

- [x] **Structured story output:** OpenAI response enforced as JSON with `title`, `moral`, `story_text`, `estimated_duration_seconds`; validated before save; rejected if structure invalid.
- [x] **Strict prompt system:** `StoryPromptBuilder` builds prompts from sanitized input only; age-based vocabulary and max word count; moral and positive tone mandatory; political, violent, religious conflict topics blocked; Tamil cultural alignment when language=ta; no raw user concatenation.
- [x] **Content moderation layer:** `StoryModerationService` runs moderation before save; rejects if flagged; logs moderation result (promptId, language, age, safe, categoriesFlagged).
- [x] **Safety validation rules:** Custom validators for harmful language, adult themes, complex psychological themes, and fear-inducing words for age &lt; 6; on validation failure retry with fallback prompt, then mark story FAILED.
- [x] **Retry and resilience:** Exponential backoff (max 2 retries); fallback simple story prompt; Resilience4j circuit breaker for OpenAI API.
- [x] **Token and cost control:** Token usage logged and stored in `story_token_usage`; max_tokens enforced; identical prompts cached in Redis.
- [x] **Story status flow:** REQUESTED → GENERATING → MODERATION_CHECK → PENDING (then audio: PENDING → PROCESSING → READY) or FAILED; transitions atomic and transactional.
- [x] **Observability:** Metrics for story_generation_latency, moderation_failure_count, fallback_usage_count, token usage distribution, hallucination_detection_rate.
- [x] **Structured logging:** JSON-friendly fields (promptId, language, age, tokenUsage, moderationSafe, retryCount) via Logstash encoder in prod/staging.

### 17.9 SDLC

- [ ] **Code review** includes security and privacy (no PII in logs; correct authz).
- [ ] **DPIA** for new high-risk processing (children, AI).
- [ ] **Dependency scan** in CI; no high/critical unmitigated.

---

## Document control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Feb 2026 | Compliance / Security | Initial investor- and regulator-ready compliance document |

**Next review:** Annual or upon material product/regulatory change.

---

*This document is part of Tamixa’s compliance and security framework. Implementations in code (e.g. `ChildService`, `StructuredAuditLogger`, `AesEncryptionService`, `SecurityConfig`) are referenced where they support the stated controls.*
