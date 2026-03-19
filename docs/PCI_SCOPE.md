# PCI-DSS Scope Statement

**Document type:** Compliance / Audit  
**Product:** Tamixa  
**Last updated:** March 2026

## Scope summary

Tamixa does **not** store, process, or transmit cardholder data (CHD). Payment card data is handled entirely by **Stripe**. Our in-scope handling is limited to:

- Redirecting users to Stripe Checkout (or embedding Stripe.js) for subscription sign-up and payment.
- Receiving and processing **webhooks** from Stripe (signature-verified) to update subscription state in our systems.
- Storing only non-CHD subscription metadata (e.g. plan, status, external subscription ID) in our database.

We do **not** have access to full card numbers, card verification codes, or magnetic-stripe data. We rely on Stripe’s PCI-DSS compliant environment and their Attestation of Compliance (AOC) for the cardholder data environment.

## Applicable SAQ

For merchants that only redirect to a PCI-compliant payment page and do not handle CHD, the **SAQ A** (or equivalent) pathway typically applies. Tamixa’s implementation (Stripe Checkout / Stripe.js, no CHD on our systems) aligns with this. We do not require a full PCI audit of our own systems for the card environment; we maintain this scope statement and ensure that:

- Stripe webhook endpoints are secured (signature verification required; see `WebhookController` and `WebhookSignatureVerifier`).
- Webhook payloads that may contain payment-related metadata are encrypted at rest in production when Stripe is enabled (see `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` and `docs/COMPLIANCE.md` Section 2.3).

## Evidence for auditors

- **No CHD storage:** No card number, CVV, or track data in our databases or logs.
- **Stripe as payment processor:** Stripe is PCI-DSS compliant; we use their hosted checkout and API.
- **Webhook security:** Signature verification and optional payload encryption at rest; idempotency to prevent replay.
- **Internal policy:** See `docs/COMPLIANCE.md` (Compliance Status & Auditor Feedback, PCI-DSS subsection).

For questions, contact compliance or security at the address in the app or in `docs/INCIDENT_RESPONSE.md`.
