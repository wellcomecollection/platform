/* Configures an SNS trigger for a Lambda */
resource "random_id" "statement_id" {
  keepers = {
    # Generate a new id each time we switch to a new topic subscription
    aws_sns_topic_subscription = "${aws_sns_topic_subscription.topic_lambda.id}"
  }

  byte_length = 8
}

resource "aws_lambda_permission" "allow_sns_trigger" {
  statement_id  = "${random_id.statement_id.hex}"
  action        = "lambda:InvokeFunction"
  function_name = "${var.lambda_function_arn}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${var.sns_trigger_arn}"
  depends_on    = ["aws_sns_topic_subscription.topic_lambda"]
}

resource "aws_sns_topic_subscription" "topic_lambda" {
  topic_arn = "${var.sns_trigger_arn}"
  protocol  = "lambda"
  endpoint  = "${var.lambda_function_arn}"
}
