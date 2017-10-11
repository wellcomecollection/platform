resource "aws_s3_bucket" "infra" {
  bucket = "${var.infra_bucket}"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "tmp"
    prefix  = "tmp/"
    enabled = true

    expiration {
      days = 30
    }
  }

  lifecycle_rule {
    id      = "elasticdump"
    prefix  = "elasticdump/"
    enabled = true

    expiration {
      days = 30
    }
  }
}

resource "aws_s3_bucket" "alb-logs" {
  bucket = "wellcomecollection-alb-logs"
  acl    = "private"

  policy = "${data.aws_iam_policy_document.alb_logs.json}"

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
