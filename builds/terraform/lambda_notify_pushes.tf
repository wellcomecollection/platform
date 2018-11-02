module "ecr_pushes_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "ecr_pushes"
}

module "lambda_pushes_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "lambda_pushes"
}

module "lambda_notify_pushes" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/builds/notify_pushes.zip"

  name        = "notify_pushes"
  description = "Post notifications of ECR/Lambda pushes to Slack"
  timeout     = 10

  environment_variables = {
    SLACK_WEBHOOK = "${var.non_critical_slack_webhook}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  log_retention_in_days = 30
}

module "trigger_ecr_pushes" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_notify_pushes.function_name}"
  lambda_function_arn  = "${module.lambda_notify_pushes.arn}"
  sns_trigger_arn      = "${module.ecr_pushes_topic.arn}"
}

module "trigger_lambda_pushes" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_notify_pushes.function_name}"
  lambda_function_arn  = "${module.lambda_notify_pushes.arn}"
  sns_trigger_arn      = "${module.lambda_pushes_topic.arn}"
}
