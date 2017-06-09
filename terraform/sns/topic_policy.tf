data "aws_iam_policy_document" "publish_to_topic" {
  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      "${aws_sns_topic.topic.arn}",
    ]
  }
}
