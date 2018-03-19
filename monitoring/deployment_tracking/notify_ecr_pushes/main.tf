module "lambda_ecr_pushes" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/deployment_tracking/notify_ecr_pushes.zip"

  name        = "notify_ecr_pushes"
  description = "Post notifications of ECR pushes to Slack"
  timeout     = 10

  environment_variables = {
    SLACK_WEBHOOK = "${var.slack_webhook}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

data "aws_sns_topic" "trigger_topic" {
  name = "${var.ecr_pushes_topic_name}"
}

module "trigger_ecr_pushes" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_ecr_pushes.function_name}"
  lambda_function_arn  = "${module.lambda_ecr_pushes.arn}"
  sns_trigger_arn      = "${data.aws_sns_topic.trigger_topic.arn}"
}
