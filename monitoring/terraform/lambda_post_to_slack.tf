module "lambda_post_to_slack" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  s3_key = "lambdas/monitoring/post_to_slack.zip"

  name        = "post_to_slack"
  description = "Post notification to Slack when an alarm is triggered"

  environment_variables = {
    SLACK_INCOMING_WEBHOOK = "${var.slack_webhook}"
    BITLY_ACCESS_TOKEN     = "${var.bitly_access_token}"
  }

  alarm_topic_arn = "${data.terraform_remote_state.lambdas.lambda_error_alarm_arn}"
}

module "trigger_post_to_slack_dlqs_not_empty" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.dlq_alarm_arn}"
}

module "trigger_post_to_slack_esg_not_terminating" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.ec2_instance_terminating_for_too_long_alarm_arn}"
}

module "trigger_post_to_slack_server_error_alb" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.alb_server_error_alarm_arn}"
}

module "trigger_post_to_slack_lambda_error" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.lambdas.lambda_error_alarm_arn}"
}

module "trigger_post_to_slack_terminal_failure" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.terminal_failure_alarm_arn}"
}
