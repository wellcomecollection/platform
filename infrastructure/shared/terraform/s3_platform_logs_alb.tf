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

data "aws_iam_policy_document" "s3_alb_logs" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::${local.alb_logs_bucket_name}/*",
    ]

    # This is the Account ID for Elastic Load Balancing; not another
    # AWS account at Wellcome.
    # See https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/enable-access-logs.html
    principals {
      identifiers = ["arn:aws:iam::156460612806:root"]
      type        = "AWS"
    }
  }
}