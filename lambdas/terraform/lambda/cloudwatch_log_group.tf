resource "aws_cloudwatch_log_group" "cloudwatch_log_group" {
  name = "/aws/lambda/${var.name}"
}

resource "aws_iam_role_policy" "cloudwatch_logs" {
  name   = "${aws_iam_role.iam_role.name}_lambda_cloudwatch_logs"
  role   = "${aws_iam_role.iam_role.name}"
  policy = "${data.aws_iam_policy_document.cloudwatch_logs.json}"
}

data "aws_iam_policy_document" "cloudwatch_logs" {
  statement {
    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = [
      "${aws_cloudwatch_log_group.cloudwatch_log_group.arn}",
    ]
  }
}
