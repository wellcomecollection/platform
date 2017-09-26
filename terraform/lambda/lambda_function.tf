data "aws_s3_bucket_object" "package" {
  bucket = "${var.s3_bucket}"
  key    = "${var.s3_key}"
}

resource "aws_lambda_function" "lambda_function" {
  description   = "${var.description}"
  function_name = "${var.name}"

  s3_bucket         = "${var.s3_bucket}"
  s3_key            = "${var.s3_key}"
  s3_object_version = "${data.aws_s3_bucket_object.package.version_id}"

  role    = "${aws_iam_role.iam_role.arn}"
  handler = "${var.name}.main"
  runtime = "python3.6"
  timeout = "${var.timeout}"

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
