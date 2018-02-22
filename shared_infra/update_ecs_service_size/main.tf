# Lambda for updating ECS service size

module "lambda_update_ecs_service_size" {
  source      = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  name        = "update_ecs_service_size"
  description = "Update the desired count of an ECS service"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/shared_infra/update_ecs_service_size.zip"
}

module "trigger_update_ecs_service_size" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_update_ecs_service_size.function_name}"
  lambda_function_arn  = "${module.lambda_update_ecs_service_size.arn}"
  sns_trigger_arn      = "${var.service_scheduler_topic_arn}"
}
