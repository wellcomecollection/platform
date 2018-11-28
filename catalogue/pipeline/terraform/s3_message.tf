locals {
  messages_bucket_name = "wellcomecollection-platform-messages"
}

resource "aws_s3_bucket" "messages" {
  bucket = "${local.messages_bucket_name}"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_messages"
    enabled = true

    expiration {
      // SQS messages are kept in queues for 4 days and then discarded,
      // so setting the expiration of messages in s3 to be the same
      days = 4
    }
  }
}
