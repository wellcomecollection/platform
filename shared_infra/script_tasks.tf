module "loris_cache_cleaner" {
  source           = "../terraform/ecs_script_task"
  task_name        = "loris_cache_cleaner"
  app_uri          = "${module.ecr_repository_cache_cleaner.repository_url}:${var.release_ids["cache_cleaner"]}"
  task_role_arn    = "${module.ecs_cache_cleaner_iam.task_role_arn}"
  volume_name      = "loris"
  volume_host_path = "${module.api_userdata.efs_mount_directory}/loris"

  cpu    = 128
  memory = 128

  env_vars = [
    "{\"name\": \"MAX_AGE\", \"value\": \"30\"}",
    "{\"name\": \"MAX_SIZE\", \"value\": \"10G\"}",
  ]
}

module "elasticdump" {
  source        = "../terraform/ecs_script_task"
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

module "update_api_docs" {
  source        = "../terraform/ecs_script_task"
  task_name     = "update_api_docs"
  app_uri       = "${module.ecr_repository_update_api_docs.repository_url}:${var.release_ids["update_api_docs"]}"
  task_role_arn = "${module.ecs_update_api_docs_iam.task_role_arn}"

  cpu    = 256
  memory = 256
}
