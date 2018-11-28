resource "aws_sns_topic" "goobi_notifications_topic" {
  name   = "s3-goobi-event-notifications"
  policy = "${data.aws_iam_policy_document.publish_to_topic.json}"
}

data "aws_iam_policy_document" "publish_to_topic" {
  statement {
    actions = [
      "sns:Publish",
    ]

    principals = {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    resources = [
      "arn:aws:sns:eu-west-1:${data.aws_caller_identity.current.account_id}:s3-goobi-event-notifications",
    ]

    condition = {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = ["${aws_s3_bucket.goobi_adapter.arn}"]
    }
  }
}
