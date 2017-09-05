# Lambda for updating ECS service size

module "lambda_update_ecs_service_size" {
  source      = "../../terraform/lambda"
  name        = "update_ecs_service_size"
  description = "Update the desired count of an ECS service"
  source_dir  = "${path.module}/target"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_update_ecs_service_size" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_update_ecs_service_size.function_name}"
  lambda_function_arn  = "${module.lambda_update_ecs_service_size.arn}"
  sns_trigger_arn      = "${var.service_scheduler_topic_arn}"
}
