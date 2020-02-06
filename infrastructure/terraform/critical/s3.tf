resource "aws_s3_bucket" "api_root_cf_logs" {
  bucket = "weco-cloudfront-logs"
  acl    = "private"

  lifecycle_rule {
    id      = "cf-logs"
    prefix  = ""
    enabled = true

    expiration {
      days = 30
    }
  }
}
