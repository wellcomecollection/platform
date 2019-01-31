resource "aws_s3_bucket" "recorder_vhs" {
  bucket = "wellcomecollection-${var.namespace}-recorder-vhs"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "messages" {
  bucket = "wellcomecollection-${var.namespace}-messages"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_messages"
    enabled = true

    expiration {
      // SQS messages are kept in queues for 4 days
      // expiration of messages in s3 should be the same
      days = 4
    }
  }
}
