# S3 audio streaming for Tamixa production
# Run from this directory: terraform init && terraform apply

variable "bucket_name" {
  description = "S3 bucket name for narration audio"
  type        = string
  default     = "tamixa-audio"
}

variable "aws_region" {
  description = "AWS region for the bucket"
  type        = string
  default     = "us-east-1"
}

variable "enable_versioning" {
  description = "Enable S3 versioning for audio objects"
  type        = bool
  default     = true
}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_s3_bucket" "audio" {
  bucket = var.bucket_name

  tags = {
    Purpose = "tamixa-narration-audio"
  }
}

resource "aws_s3_bucket_public_access_block" "audio" {
  bucket = aws_s3_bucket.audio.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "audio" {
  count = var.enable_versioning ? 1 : 0

  bucket = aws_s3_bucket.audio.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_cors_configuration" "audio" {
  bucket = aws_s3_bucket.audio.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = ["Content-Range", "Content-Length"]
  }
}

resource "aws_iam_user" "audio_backend" {
  name = "tamixa-audio-backend"

  tags = {
    Purpose = "tamixa-narration-audio"
  }
}

resource "aws_iam_access_key" "audio_backend" {
  user = aws_iam_user.audio_backend.name
}

resource "aws_iam_user_policy" "audio_backend" {
  name = "tamixa-audio-s3-access"
  user = aws_iam_user.audio_backend.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
        Resource = "${aws_s3_bucket.audio.arn}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = aws_s3_bucket.audio.arn
      }
    ]
  })
}

output "bucket_name" {
  value = aws_s3_bucket.audio.id
}

output "aws_region" {
  value = var.aws_region
}

output "access_key_id" {
  value     = aws_iam_access_key.audio_backend.id
  sensitive = true
}

output "secret_access_key" {
  value     = aws_iam_access_key.audio_backend.secret
  sensitive = true
}
