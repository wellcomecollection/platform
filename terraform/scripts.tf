module "loris_cache_cleaner" {
  source           = "./ecs_script_task"
  task_name        = "loris_cache_cleaner"
  app_uri          = "${module.ecr_repository_cache_cleaner.repository_url}:${var.release_ids["cache_cleaner"]}"
  task_role_arn    = "${module.ecs_cache_cleaner_iam.task_role_arn}"
  volume_name      = "loris"
  volume_host_path = "${module.api_userdata.efs_mount_directory}/loris"
  container_path   = "/data"

  cpu    = 256
  memory = 256

  env_vars = [
    "{\"name\": \"MAX_AGE\", \"value\": \"30\"}",
    "{\"name\": \"MAX_SIZE\", \"value\": \"10G\"}",
  ]
}

module "gatling" {
  source        = "./ecs_script_task"
  task_name     = "gatling"
  app_uri       = "${module.ecr_repository_gatling.repository_url}:${var.release_ids["gatling"]}"
  task_role_arn = "${module.ecs_gatling_iam.task_role_arn}"

  cpu    = 256
  memory = 256

  env_vars = [
    "{\"name\": \"SIMULATION\", \"value\": \"testing.load.LorisSimulation\"}",
  ]
}
