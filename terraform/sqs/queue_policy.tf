data "aws_iam_policy_document" "sqs_queue_policy" {
  statement {
    sid    = "es-sns-to-sqs-policy"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }

    actions = [
      "sqs:SendMessage",
    ]

    resources = [
      "arn:aws:sqs:${var.aws_region}:${var.account_id}:${var.queue_name}",
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"

      values = ["${formatlist("arn:aws:sns:%s:%s:%s",var.aws_region, var.account_id, var.topic_names)}"]
    }
  }
}
