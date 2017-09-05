# Lambda for tagging EC2 instances with ECS cluster/container instance id

module "lambda_ecs_ec2_instance_tagger" {
  source     = "../../terraform/lambda"
  source_dir = "${path.module}/target"

  name        = "ecs_ec2_instance_tagger"
  description = "Tag an EC2 instance with ECS cluster/container instance id"
  timeout     = 10

  environment_variables = {
    BUCKET_NAME = "${var.bucket_infra_id}"
    OBJECT_PATH = "tmp/ecs_ec2_instance_tagger"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_ecs_ec2_instance_tagger" {
  source                  = "../../terraform/lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_ecs_ec2_instance_tagger.function_name}"
  lambda_function_arn     = "${module.lambda_ecs_ec2_instance_tagger.arn}"
  cloudwatch_trigger_arn  = "${var.ecs_container_instance_state_change_arn}"
  cloudwatch_trigger_name = "${var.ecs_container_instance_state_change_name}"
  custom_input            = false
}
