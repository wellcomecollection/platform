locals {
  messages_bucket_name = "wellcomecollection-platform-messages"
}

resource "aws_s3_bucket" "messages" {
  bucket = "${local.messages_bucket_name}"
  acl    = "private"

  policy = "${data.aws_iam_policy_document.allow_s3_messages_put_get.json}"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_messages"
    enabled = true

    expiration {
      days = 4
    }
  }
}