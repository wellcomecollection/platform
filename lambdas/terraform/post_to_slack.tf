# Lambda for posting on slack when an alarm is triggered

module "lambda_post_to_slack" {
  source      = "./lambda"
  name        = "post_to_slack"
  description = "Post notification to Slack when an alarm is triggered"
  source_dir  = "../src/post_to_slack"

  environment_variables = {
    SLACK_INCOMING_WEBHOOK = "${var.slack_webhook}"
  }

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_post_to_slack_dlqs_not_empty" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.dlq_alarm_arn}"
}

module "trigger_post_to_slack_esg_not_terminating" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.ec2_instance_terminating_for_too_long_alarm_arn}"
}

module "trigger_post_to_slack_server_error_alb" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.alb_server_error_alarm_arn}"
}

module "trigger_post_to_slack_lambda_error" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_post_to_slack_terminal_failure" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.terminal_failure_alarm_arn}"
}
