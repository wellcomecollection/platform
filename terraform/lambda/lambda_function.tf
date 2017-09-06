data "archive_file" "lambda_zip_file" {
  type        = "zip"
  source_dir  = "${var.source_dir}"
  output_path = "${var.source_dir}/../${var.name}.zip"
}

resource "aws_lambda_function" "lambda_function" {
  description      = "${var.description}"
  filename         = "${data.archive_file.lambda_zip_file.output_path}"
  function_name    = "${var.name}"
  role             = "${aws_iam_role.iam_role.arn}"
  handler          = "${var.name}.main"
  runtime          = "python3.6"
  source_code_hash = "${data.archive_file.lambda_zip_file.output_base64sha256}"
  timeout          = "${var.timeout}"

  dead_letter_config = {
    target_arn = "${aws_sqs_queue.lambda_dlq.arn}"
  }

  environment {
    variables = "${var.environment_variables}"
  }
}

resource "aws_cloudwatch_metric_alarm" "lambda_alarm" {
  alarm_name          = "lambda-${var.name}-errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"

  dimensions {
    FunctionName = "${var.name}"
  }

  alarm_description = "This metric monitors lambda errors for function: ${var.name}"
  alarm_actions     = ["${var.alarm_topic_arn}"]
}
