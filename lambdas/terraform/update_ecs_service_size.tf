# Lambda for updating ECS service size

module "lambda_update_ecs_service_size" {
  source      = "./lambda"
  name        = "update_ecs_service_size"
  description = "Update the desired count of an ECS service"
  source_dir  = "../src/update_ecs_service_size"

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_update_ecs_service_size" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_update_ecs_service_size.function_name}"
  lambda_function_arn  = "${module.lambda_update_ecs_service_size.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.service_scheduler_topic_arn}"
}
