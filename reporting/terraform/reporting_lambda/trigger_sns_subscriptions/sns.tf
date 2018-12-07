/* Configures an SNS trigger for a Lambda */
resource "random_id" "statement_id" {
  keepers = {
    # Generate a new id each time we switch to a new topic subscription
    aws_sns_topic_subscription = "${aws_sns_topic_subscription.topic_lambda.id}"
  }

  byte_length = 8
}

data "aws_lambda_function" "function" {
  function_name = "${var.lambda_function_name}"
}

resource "aws_lambda_permission" "allow_sns_trigger" {
  count         = "${var.topic_count}"
  statement_id  = "${random_id.statement_id.hex}"
  action        = "lambda:InvokeFunction"
  function_name = "${data.aws_lambda_function.function.function_name}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${element(var.topic_arns, count.index)}"
  depends_on    = ["aws_sns_topic_subscription.topic_lambda"]
}

resource "aws_sns_topic_subscription" "topic_lambda" {
  count     = "${var.topic_count}"
  protocol  = "lambda"
  topic_arn = "${element(var.topic_arns, count.index)}"
  endpoint  = "${data.aws_lambda_function.function.arn}"
}