resource "aws_s3_bucket" "sierra_adapter" {
  bucket = "wellcomecollection-platform-adapters-sierra"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_old_items"
    prefix  = "records_items/"
    enabled = true

    expiration {
      days = 7
    }
  }

  lifecycle_rule {
    id      = "expire_old_bibs"
    prefix  = "records_bibs/"
    enabled = true

    expiration {
      days = 7
    }
  }
}
