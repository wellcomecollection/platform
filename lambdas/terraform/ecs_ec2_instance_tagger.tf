# Lambda for tagging EC2 instances with ECS cluster/container instance id

module "lambda_ecs_ec2_instance_tagger" {
  source      = "./lambda"
  name        = "ecs_ec2_instance_tagger"
  description = "Tag an EC2 instance with ECS cluster/container instance id"
  source_dir  = "../src/ecs_ec2_instance_tagger"
  timeout     = 10

  environment_variables = {
    BUCKET_NAME = "${data.terraform_remote_state.platform.bucket_infra_id}"
    OBJECT_PATH = "tmp/ecs_ec2_instance_tagger"
  }

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_ecs_ec2_instance_tagger" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_ecs_ec2_instance_tagger.function_name}"
  lambda_function_arn     = "${module.lambda_ecs_ec2_instance_tagger.arn}"
  cloudwatch_trigger_arn  = "${data.terraform_remote_state.platform.ecs_container_instance_state_change_arn}"
  cloudwatch_trigger_name = "${data.terraform_remote_state.platform.ecs_container_instance_state_change_name}"
  custom_input            = false
}
