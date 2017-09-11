module "scheduled_task_gatling_catalogue_api" {
  source = "../terraform/ecs_task_schedule"

  cloudwatch_event_rule_name = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
  cluster_arn                = "${aws_ecs_cluster.services.id}"
  task_definition_arn        = "${module.gatling_catalogue_api.task_arn}"
}

module "scheduled_task_gatling_loris" {
  source = "../terraform/ecs_task_schedule"

  cloudwatch_event_rule_name = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
  cluster_arn                = "${aws_ecs_cluster.services.id}"
  task_definition_arn        = "${module.gatling_loris.task_arn}"
}
