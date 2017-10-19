module "sierra_adapter_task" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "sierra_adapter"
  app_uri       = "${module.ecr_repository_sierra_adapter.repository_url}:${var.release_ids["sierra_adapter"]}"
  task_role_arn = "${module.sierra_adapter_iam.task_role_arn}"

  env_vars = [
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}
