resource "aws_sqs_queue" "lambda_dlq" {
  name = "lambda-${var.name}_dlq"
}

resource "aws_iam_role_policy" "lambda_dlq" {
  name   = "${aws_iam_role.iam_role.name}_lambda_dlq"
  role   = "${aws_iam_role.iam_role.name}"
  policy = "${data.aws_iam_policy_document.lambda_dlq.json}"
}

data "aws_iam_policy_document" "lambda_dlq" {
  statement {
    actions = [
      "sqs:SendMessage",
    ]

    resources = [
      "${aws_sqs_queue.lambda_dlq.arn}",
    ]
  }
}
