-- Migrate payment provider from Razorpay to Zoho Payments.
-- Run before deploying Zoho integration; existing RAZORPAY records become ZOHO.
UPDATE subscriptions SET provider = 'ZOHO' WHERE provider = 'RAZORPAY';
UPDATE subscription_events SET provider = 'ZOHO' WHERE provider = 'RAZORPAY';
UPDATE billing_audit_log SET provider = 'ZOHO' WHERE provider = 'RAZORPAY';
UPDATE webhook_idempotency SET provider = 'ZOHO' WHERE provider = 'RAZORPAY';
UPDATE webhook_events SET provider = 'ZOHO' WHERE provider = 'RAZORPAY';
