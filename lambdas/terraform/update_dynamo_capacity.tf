# Lambda for updating the capacity of a DynamoDB table

module "lambda_update_dynamo_capacity" {
  source      = "./lambda"
  name        = "update_dynamo_capacity"
  description = "Update the capacity of a DynamoDB table"
  source_dir  = "../src/update_dynamo_capacity"

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_update_dynamo_capacity" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_update_dynamo_capacity.function_name}"
  lambda_function_arn  = "${module.lambda_update_dynamo_capacity.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.dynamo_capacity_topic_arn}"
}

module "lambda_drain_ecs_container_instance" {
  source      = "./lambda"
  name        = "drain_ecs_container_instance"
  description = "Drain ECS container instance when the corresponding EC2 instance is being terminated"
  source_dir  = "../src/drain_ecs_container_instance"
  timeout     = 60

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_drain_ecs_container_instance" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_drain_ecs_container_instance.function_name}"
  lambda_function_arn  = "${module.lambda_drain_ecs_container_instance.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.ec2_terminating_topic_arn}"
}
