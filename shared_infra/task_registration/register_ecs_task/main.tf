# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_register_ecs_task" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "register_ecs_task"
  description = "Register an ECS task from a message published to SNS"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/shared_infra/register_ecs_task.zip"
}

module "trigger_run_ecs_task" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_register_ecs_task.function_name}"
  lambda_function_arn  = "${module.lambda_register_ecs_task.arn}"
  sns_trigger_arn      = "${module.register_ecs_task_topic.arn}"
}
