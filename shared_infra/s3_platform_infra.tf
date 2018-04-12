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
