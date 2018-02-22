# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_service_scheduler" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "service_scheduler"
  description = "Publish an ECS service schedule to SNS"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/shared_infra/service_scheduler.zip"
}
