module "slack_budget_bot" {
  source        = "git::https://github.com/wellcometrust/terraform-modules.git//ecs_script_task?ref=v1.0.0"
  task_name     = "slack_budget_bot"
  app_uri       = "${module.ecr_repository_slack_budget_bot.repository_url}:${var.release_ids["slack_budget_bot"]}"
  task_role_arn = "${module.ecs_slack_budget_bot_iam.task_role_arn}"

  env_vars = [
    "{\"name\": \"S3_BUCKET\", \"value\": \"${var.dashboard_bucket_id}\"}",
    "{\"name\": \"ACCOUNT_ID\", \"value\": \"${var.account_id}\"}",
    "{\"name\": \"SLACK_WEBHOOK\", \"value\": \"${var.slack_webhook}\"}",
  ]
}

module "ecs_slack_budget_bot_iam" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs_iam?ref=v1.0.0"
  name   = "slack_budget_bot"
}

module "ecr_repository_slack_budget_bot" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "slack_budget_bot"
}

data "aws_ecs_cluster" "services" {
  cluster_name = "${var.ecs_services_cluster_id}"
}

module "scheduled_slack_budget" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//ecs_task_schedule?ref=v1.0.0"

  cloudwatch_event_rule_name = "${aws_cloudwatch_event_rule.every_day_at_8am.name}"
  cluster_arn                = "${data.aws_ecs_cluster.services.arn}"
  task_definition_arn        = "${module.slack_budget_bot.task_definition_arn}"
}

resource "aws_cloudwatch_event_rule" "every_day_at_8am" {
  name                = "every_day_at_8am"
  schedule_expression = "cron(0 8 * * ? *)"
}
