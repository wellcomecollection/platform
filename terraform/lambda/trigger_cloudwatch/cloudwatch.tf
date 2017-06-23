/* Configures a Cloudwatch trigger for a Lambda */

resource "aws_lambda_permission" "allow_cloudwatch_trigger" {
  statement_id  = "AllowExecutionFromCloudWatch_${var.lambda_function_name}"
  action        = "lambda:InvokeFunction"
  function_name = "${var.lambda_function_name}"
  principal     = "events.amazonaws.com"
  source_arn    = "${var.cloudwatch_trigger_arn}"
}

resource "aws_cloudwatch_event_target" "event_trigger" {
  count = "${var.custom_input}"

  rule  = "${var.cloudwatch_trigger_name}"
  arn   = "${var.lambda_function_arn}"
  input = "${var.input}"
}

resource "aws_cloudwatch_event_target" "event_trigger" {
  count = "${1 - var.custom_input}"

  rule  = "${var.cloudwatch_trigger_name}"
  arn   = "${var.lambda_function_arn}"
}