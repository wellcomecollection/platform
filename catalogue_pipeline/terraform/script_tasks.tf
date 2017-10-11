module "elasticdump" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "elasticdump"
  app_uri       = "${module.ecr_repository_elasticdump.repository_url}:${var.release_ids["elasticdump"]}"
  task_role_arn = "${module.ecs_elasticdump_iam.task_role_arn}"

  cpu    = 1024
  memory = 1024

  env_vars = [
    "{\"name\": \"BUCKET\", \"value\": \"${var.infra_bucket}\"}",
    "{\"name\": \"CONFIG_KEY\", \"value\": \"${module.ingestor.config_key}\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}
