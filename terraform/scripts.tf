module "loris_cache_cleaner" {
  source           = "./ecs_script_task"
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

module "gatling_loris" {
  source        = "./ecs_script_task"
  task_name     = "gatling_loris"
  app_uri       = "${module.ecr_repository_gatling.repository_url}:${var.release_ids["gatling"]}"
  task_role_arn = "${module.ecs_gatling_iam.task_role_arn}"

  cpu    = 1024
  memory = 1024

  env_vars = [
    "{\"name\": \"SIMULATION\", \"value\": \"testing.load.LorisSimulation\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
    "{\"name\": \"FAILED_TOPIC_ARN\", \"value\": \"${module.load_test_failure_alarm.arn}\"}",
    "{\"name\": \"RESULTS_TOPIC_ARN\", \"value\": \"${module.load_test_results.arn}\"}",
    "{\"name\": \"S3_BUCKET\", \"value\": \"${aws_s3_bucket.dashboard.id}\"}",
  ]
}

module "gatling_catalogue_api" {
  source        = "./ecs_script_task"
  task_name     = "gatling_catalogue_api"
  app_uri       = "${module.ecr_repository_gatling.repository_url}:${var.release_ids["gatling"]}"
  task_role_arn = "${module.ecs_gatling_iam.task_role_arn}"

  cpu    = 1024
  memory = 1024

  env_vars = [
    "{\"name\": \"SIMULATION\", \"value\": \"testing.load.CatalogueApiSimulation\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
    "{\"name\": \"FAILED_TOPIC_ARN\", \"value\": \"${module.load_test_failure_alarm.arn}\"}",
    "{\"name\": \"RESULTS_TOPIC_ARN\", \"value\": \"${module.load_test_results.arn}\"}",
    "{\"name\": \"S3_BUCKET\", \"value\": \"${aws_s3_bucket.dashboard.id}\"}",
  ]
}

module "gatling_digital_experience" {
  source        = "./ecs_script_task"
  task_name     = "gatling_digital_experience"
  app_uri       = "${module.ecr_repository_gatling.repository_url}:${var.release_ids["gatling"]}"
  task_role_arn = "${module.ecs_gatling_iam.task_role_arn}"

  cpu    = 1024
  memory = 1024

  env_vars = [
    "{\"name\": \"SIMULATION\", \"value\": \"testing.load.DigitalExperienceImageSearch\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
    "{\"name\": \"FAILED_TOPIC_ARN\", \"value\": \"${module.load_test_failure_alarm.arn}\"}",
    "{\"name\": \"RESULTS_TOPIC_ARN\", \"value\": \"${module.load_test_results.arn}\"}",
    "{\"name\": \"S3_BUCKET\", \"value\": \"${aws_s3_bucket.dashboard.id}\"}",
  ]
}

module "elasticsearch_cleaner" {
  source        = "./ecs_script_task"
  task_name     = "elasticsearch_cleaner"
  app_uri       = "${module.ecr_repository_elasticsearch_cleaner.repository_url}:${var.release_ids["elasticsearch_cleaner"]}"
  task_role_arn = "${module.ecs_elasticsearch_cleaner_iam.task_role_arn}"

  cpu    = 512
  memory = 512

  env_vars = [
    "{\"name\": \"DRY_RUN\", \"value\": \"false\"}",
    "{\"name\": \"KEY\", \"value\": \"terraform.tfvars\"}",
    "{\"name\": \"BUCKET\", \"value\": \"${var.infra_bucket}\"}",
  ]
}
