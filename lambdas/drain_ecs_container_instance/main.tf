module "lambda_drain_ecs_container_instance" {
  source     = "../../terraform/lambda"
  source_dir = "${path.module}/target"

  name        = "drain_ecs_container_instance"
  description = "Drain ECS container instance when the corresponding EC2 instance is being terminated"
  timeout     = 60

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_drain_ecs_container_instance" {
  source = "../../terraform/lambda/trigger_sns"

  lambda_function_name = "${module.lambda_drain_ecs_container_instance.function_name}"
  lambda_function_arn  = "${module.lambda_drain_ecs_container_instance.arn}"
  sns_trigger_arn      = "${var.ec2_terminating_topic_arn}"
}
