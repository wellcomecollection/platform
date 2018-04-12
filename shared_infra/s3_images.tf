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
