# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_service_scheduler" {
  source      = "./lambda"
  name        = "service_scheduler"
  description = "Publish an ECS service schedule to SNS"
  source_dir  = "../src/service_scheduler"

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}
