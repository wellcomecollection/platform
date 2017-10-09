module "scheduled_task_gatling_catalogue_api" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_task_schedule?ref=v1.0.0"

  cloudwatch_event_rule_name = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
  cluster_arn                = "${data.terraform_remote_state.platform.aws_ecs_cluster_services_id}"
  task_definition_arn        = "${module.gatling_catalogue_api.task_definition_arn}"
}

module "scheduled_task_gatling_loris" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_task_schedule?ref=v1.0.0"

  cloudwatch_event_rule_name = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
  cluster_arn                = "${data.terraform_remote_state.platform.aws_ecs_cluster_services_id}"
  task_definition_arn        = "${module.gatling_loris.task_definition_arn}"
}
