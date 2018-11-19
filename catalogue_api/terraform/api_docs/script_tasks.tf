module "update_api_docs" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "update_api_docs"
  app_uri       = "${var.container_uri}"
  task_role_arn = "${module.ecs_update_api_docs_iam.task_role_arn}"

  cpu    = 256
  memory = 256
}
