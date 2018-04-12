resource "aws_s3_bucket" "cloudfront_logs" {
  bucket = "wellcomecollection-platform-logs-cloudfront"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_cloudfront_logs"
    enabled = true

    expiration {
      days = 30
    }
  }
}
