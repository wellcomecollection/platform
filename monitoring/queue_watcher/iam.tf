data "aws_iam_policy_document" "read_queue_attributes" {
  statement {
    actions = [
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
      "sqs:ListDeadLetterSourceQueues",
      "sqs:ListQueues",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "write_cloudwatch" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "write_cloudwatch" {
  role   = "${module.lambda_queue_watcher.role_name}"
  policy = "${data.aws_iam_policy_document.write_cloudwatch.json}"
}

resource "aws_iam_role_policy" "read_queue_attributes" {
  role   = "${module.lambda_queue_watcher.role_name}"
  policy = "${data.aws_iam_policy_document.read_queue_attributes.json}"
}
