# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_run_ecs_task" {
  source = "../../terraform/lambda"

  name        = "run_ecs_task"
  description = "Run an ECS task from a message published to SNS"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/run_ecs_task.zip"
}

module "trigger_run_ecs_task" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_run_ecs_task.function_name}"
  lambda_function_arn  = "${module.lambda_run_ecs_task.arn}"
  sns_trigger_arn      = "${module.run_ecs_task_topic.arn}"
}
