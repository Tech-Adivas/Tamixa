# S3 audio streaming (production)

Provisions a private S3 bucket and IAM user for narration audio.

## Prerequisites

- AWS CLI configured (`aws configure`) or env vars: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- Terraform ≥ 1.0

## Usage

```bash
cd infra/terraform/s3
terraform init
terraform plan
terraform apply
```

## Outputs

After apply, set these in your production environment:

- `bucket_name` → `S3_BUCKET`
- `aws_region` → `S3_REGION`
- `access_key_id` → `AWS_ACCESS_KEY_ID`
- `secret_access_key` → `AWS_SECRET_ACCESS_KEY` (sensitive)

Also set `STORAGE_TYPE=s3` in production.
