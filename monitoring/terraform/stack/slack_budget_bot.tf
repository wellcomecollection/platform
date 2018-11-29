module "slack_budget_bot" {
  source    = "git::https://github.com/wellcometrust/terraform-modules.git//ecs_script_task?ref=v10.2.2"
  task_name = "slack_budget_bot"
  app_uri   = "${var.slack_budget_bot_container_uri}"

  #"${module.ecr_repository_slack_budget_bot.repository_url}:${var.release_ids["slack_budget_bot"]}"

  task_role_arn = "${module.ecs_slack_budget_bot_iam.task_role_arn}"
  env_vars = [
    "{\"name\": \"S3_BUCKET\", \"value\": \"${var.monitoring_bucket}\"}",
    "{\"name\": \"ACCOUNT_ID\", \"value\": \"${var.account_id}\"}",
    "{\"name\": \"SLACK_WEBHOOK\", \"value\": \"${var.non_critical_slack_webhook}\"}",
  ]
  log_retention_in_days = 30
}

module "ecs_slack_budget_bot_iam" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs_iam?ref=v1.0.0"
  name   = "slack_budget_bot"
}

module "scheduled_slack_budget" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs_task_schedule?ref=v1.0.0"

  cloudwatch_event_rule_name = "${var.every_day_at_8am_rule_name}"
  cluster_arn                = "${aws_ecs_cluster.cluster.arn}"
  task_definition_arn        = "${module.slack_budget_bot.task_definition_arn}"
}
