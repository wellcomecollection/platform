/* Configures an SNS trigger for a Lambda

resource "aws_lambda_permission" "a" {
  count         = "${var.sns_trigger_arn == "" ? 1 : 0}"
  statement_id  = "AllowExecutionFromSNS_${var.name}"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.lambda_function.arn}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${var.sns_trigger_arn}"
}

resource "aws_sns_topic_subscription" "topic_lambda" {
  count     = "${var.sns_trigger_arn == "" ? 1 : 0}"
  topic_arn = "${var.sns_trigger_arn}"
  protocol  = "lambda"
  endpoint  = "${aws_lambda_function.lambda_function.arn}"
}

*/

