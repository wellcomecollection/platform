module "lambda_terraform_tracker" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/terraform_tracker.zip"

  name        = "terraform_tracker"
  description = "Post notifications of 'terraform apply' to Slack"
  timeout     = 10

  environment_variables = {
    INFRA_BUCKET       = "${var.monitoring_bucket}"
    SLACK_WEBHOOK      = "${var.non_critical_slack_webhook}"
    BITLY_ACCESS_TOKEN = "${var.bitly_access_token}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  log_retention_in_days = 30
}

data "aws_sns_topic" "trigger_topic" {
  name = "${var.terraform_apply_topic_name}"
}

module "trigger_terraform_tracker" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_terraform_tracker.function_name}"
  lambda_function_arn  = "${module.lambda_terraform_tracker.arn}"
  sns_trigger_arn      = "${data.aws_sns_topic.trigger_topic.arn}"
}
