resource "aws_iam_role" "iam_role" {
  name               = "lambda_${var.name}_iam_role"
  assume_role_policy = "${data.aws_iam_policy_document.assume_lambda_role.json}"
}

data "aws_iam_policy_document" "assume_lambda_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "cloudwatch_logs" {
  name   = "${aws_iam_role.iam_role.name}_lambda_cloudwatch_logs"
  role   = "${aws_iam_role.iam_role.name}"
  policy = "${data.aws_iam_policy_document.cloudwatch_logs.json}"
}

data "aws_iam_policy_document" "cloudwatch_logs" {
  statement {
    actions = [
      "logs:CreateLogGroup",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "logs:CreateLogGroup",
      "logs:PutLogEvents",
    ]

    resources = [
      "${aws_cloudwatch_log_group.cloudwatch_log_group.arn}",
    ]
  }
}
