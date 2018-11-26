# update_service_list

module "lambda_update_service_list" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/update_service_list.zip"

  name        = "update_service_list"
  description = "Publish ECS service status summary to S3"

  # We've seen timeouts at 60 seconds on these Lambdas, so set the max
  # timeout to make a timeout as unlikely as possible.
  timeout = 300

  environment_variables = {
    BUCKET_NAME     = "${aws_s3_bucket.dashboard.bucket}"
    OBJECT_KEY      = "data/ecs_status.json"
    ASSUMABLE_ROLES = "${join(",", var.dashboard_assumable_roles)}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  log_retention_in_days = 14
}

module "trigger_update_service_list" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"

  lambda_function_name    = "${module.lambda_update_service_list.function_name}"
  lambda_function_arn     = "${module.lambda_update_service_list.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.every_minute.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.every_minute.arn}"
}

# post_to_slack

module "lambda_post_to_slack" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

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

  log_retention_in_days = 30
}

module "trigger_post_to_slack_dlqs_not_empty" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${local.dlq_alarm_arn}"
}

module "trigger_post_to_slack_server_error_alb" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${local.alb_server_error_alarm_arn}"
}

module "trigger_post_to_slack_lambda_error" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_post_to_slack.function_name}"
  lambda_function_arn  = "${module.lambda_post_to_slack.arn}"
  sns_trigger_arn      = "${local.lambda_error_alarm_arn}"
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
  source_arn    = "${local.cloudfront_errors_topic_arn}"
  depends_on    = ["aws_sns_topic_subscription.subscribe_lambda_to_cloudfront_errors"]
}

resource "aws_sns_topic_subscription" "subscribe_lambda_to_cloudfront_errors" {
  provider = "aws.us_east_1"

  topic_arn = "${local.cloudfront_errors_topic_arn}"
  protocol  = "lambda"
  endpoint  = "${module.lambda_post_to_slack.arn}"
}

# Terraform tracker

module "lambda_terraform_tracker" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/terraform_tracker.zip"

  name        = "terraform_tracker"
  description = "Post notifications of 'terraform apply' to Slack"
  timeout     = 10

  environment_variables = {
    INFRA_BUCKET       = "${aws_s3_bucket.monitoring.bucket}"
    SLACK_WEBHOOK      = "${var.non_critical_slack_webhook}"
    BITLY_ACCESS_TOKEN = "${var.bitly_access_token}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  log_retention_in_days = 30
}