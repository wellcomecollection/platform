resource "aws_s3_bucket" "private_data" {
  bucket = "wellcomecollection-data-private"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_old_elasticdumps"
    enabled = true

    prefix = "elasticdump/"

    expiration {
      days = 30
    }
  }
}

