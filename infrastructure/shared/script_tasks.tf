module "sqs_freezeray" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "sqs_freezeray"
  app_uri       = "wellcome/sqs_freezeray:latest"
  task_role_arn = "${module.ecs_sqs_freezeray_iam.task_role_arn}"

  cpu    = 1024
  memory = 1024

  env_vars = [
    "{\"name\": \"BUCKET\", \"value\": \"${var.infra_bucket}\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}

module "sqs_redrive" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "sqs_redrive"
  app_uri       = "wellcome/sqs_redrive:latest"
  task_role_arn = "${module.ecs_sqs_redrive_iam.task_role_arn}"

  cpu    = 1024
  memory = 1024

  env_vars = [
    "{\"name\": \"BUCKET\", \"value\": \"${var.infra_bucket}\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}
