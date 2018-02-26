resource "aws_s3_bucket" "platform_infra" {
  bucket = "wellcomecollection-platform-infra"
  acl    = "private"

  lifecycle_rule {
    id      = "tmp"
    prefix  = "tmp/"
    enabled = true

    expiration {
      days = 30
    }
  }

  lifecycle {
    prevent_destroy = true
  }

  versioning {
    enabled = true
  }
}

locals {
  alb_logs_bucket_name = "wellcomecollection-platform-logs-alb"
}

resource "aws_s3_bucket" "alb_logs" {
  bucket = "${local.alb_logs_bucket_name}"
  acl    = "private"

  policy = "${data.aws_iam_policy_document.s3_alb_logs.json}"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_alb_logs"
    enabled = true

    expiration {
      days = 30
    }
  }
}

resource "aws_s3_bucket" "wellcomecollection-images" {
  bucket = "wellcomecollection-images"
  acl    = "private"

  # JP2 master assets (same set as catalogue API)
  lifecycle_rule {
    id     = "library"
    prefix = "library/"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }

  # JP2 master assets
  lifecycle_rule {
    id     = "cold_store"
    prefix = "cold_store/"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }

  # JP2 master assets
  lifecycle_rule {
    id     = "tandem_vault"
    prefix = "tandem_vault/"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }

  versioning {
    enabled = true
  }

  lifecycle_rule {
    enabled = true

    noncurrent_version_expiration {
      days = 30
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}
