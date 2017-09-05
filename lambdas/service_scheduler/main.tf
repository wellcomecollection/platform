# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_service_scheduler" {
  source     = "../../terraform/lambda"
  source_dir = "${path.module}/target"

  name        = "service_scheduler"
  description = "Publish an ECS service schedule to SNS"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}
