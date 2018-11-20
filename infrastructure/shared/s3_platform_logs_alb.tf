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
