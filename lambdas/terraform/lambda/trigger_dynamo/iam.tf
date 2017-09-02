data "aws_iam_policy_document" "allow_stream_access" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${var.stream_arn}",
    ]
  }
}

resource "aws_iam_role_policy" "give_lambda_stream_access" {
  role   = "${var.function_role}"
  policy = "${data.aws_iam_policy_document.allow_stream_access.json}"
}
