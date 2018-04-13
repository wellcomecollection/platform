module "lambda_post_to_slack" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/post_to_slack.zip"

  name        = "post_to_slack"
  description = "Post notification to Slack when an alarm is triggered"
  timeout     = 10

  environment_variables = {
    CRITICAL_SLACK_WEBHOOK    = "${var.critical_slack_webhook}"
    NONCRITICAL_SLACK_WEBHOOK = "${var.non_critical_slack_webhook}"
    BITLY_ACCESS_TOKEN        = "${var.bitly_access_token}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

# This one has been a little fiddly to add, as the topic is defined in a
# different region (us-east-1).
module "trigger_post_to_slack_cloudfront_errors" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${module.cloudfront_errors_topic.arn}"
}
