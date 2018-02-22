module "lambda_drain_ecs_container_instance" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "drain_ecs_container_instance"
  description = "Drain ECS container instance when the corresponding EC2 instance is being terminated"
  timeout     = 60

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/shared_infra/drain_ecs_container_instance.zip"
}

module "trigger_drain_ecs_container_instance" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_drain_ecs_container_instance.function_name}"
  lambda_function_arn  = "${module.lambda_drain_ecs_container_instance.arn}"
  sns_trigger_arn      = "${var.ec2_terminating_topic_arn}"
}
