module "ecr_repository_slack_budget_bot" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "slack_budget_bot"
}
