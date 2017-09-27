# Lambda for posting on slack when an alarm is triggered

module "lambda_post_to_slack" {
  source = "../../terraform/lambda"

  name        = "post_to_slack"
  description = "Post notification to Slack when an alarm is triggered"

  environment_variables = {
    SLACK_INCOMING_WEBHOOK = "${var.slack_webhook}"
    BITLY_ACCESS_TOKEN     = "${var.bitly_access_token}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/post_to_slack.zip"
}

module "trigger_post_to_slack_dlqs_not_empty" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${var.dlq_alarm_arn}"
}

module "trigger_post_to_slack_esg_not_terminating" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${var.ec2_instance_terminating_for_too_long_alarm_arn}"
}

module "trigger_post_to_slack_server_error_alb" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${var.alb_server_error_alarm_arn}"
}

module "trigger_post_to_slack_lambda_error" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${var.lambda_error_alarm_arn}"
}

module "trigger_post_to_slack_terminal_failure" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${var.terminal_failure_alarm_arn}"
}
