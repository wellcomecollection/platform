/* Configures a Cloudwatch trigger for a Lambda */

resource "random_id" "cloudwatch_trigger_name" {
  byte_length = 8
  prefix      = "AllowExecutionFromCloudWatch_${var.lambda_function_name}_"
}

resource "aws_lambda_permission" "allow_cloudwatch_trigger" {
  statement_id  = "${random_id.cloudwatch_trigger_name.id}"
  action        = "lambda:InvokeFunction"
  function_name = "${var.lambda_function_name}"
  principal     = "events.amazonaws.com"
  source_arn    = "${var.cloudwatch_trigger_arn}"
}

# The count here is substituting for the availability of an if statement in terraform
# See https://blog.gruntwork.io/terraform-tips-tricks-loops-if-statements-and-gotchas-f739bbae55f9
resource "aws_cloudwatch_event_target" "event_trigger_custom" {
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
