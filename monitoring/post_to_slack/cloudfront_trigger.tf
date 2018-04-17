# Because CloudFront metrics (and the alarm SNS topic) live in
# us-east-1 rather than eu-west-1, we need to jump through a few extra
# hoops to get the SNS ~> Lambda trigger set up.

provider "aws" {
  region = "us-east-1"
  alias  = "us_east_1"
}

resource "random_id" "statement_id" {
  keepers = {
    aws_sns_topic_subscription = "${aws_sns_topic_subscription.subscribe_lambda_to_cloudfront_errors.id}"
  }

  byte_length = 8
}

resource "aws_lambda_permission" "allow_sns_cloudfront_trigger" {
  statement_id  = "${random_id.statement_id.hex}"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_post_to_slack.arn}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${var.cloudfront_errors_topic_arn}"
  depends_on    = ["aws_sns_topic_subscription.subscribe_lambda_to_cloudfront_errors"]
}

resource "aws_sns_topic_subscription" "subscribe_lambda_to_cloudfront_errors" {
  provider = "aws.us_east_1"

  topic_arn = "${var.cloudfront_errors_topic_arn}"
  protocol  = "lambda"
  endpoint  = "${module.lambda_post_to_slack.arn}"
}
