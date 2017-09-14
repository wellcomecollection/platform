module "xml_to_json_converter" {
  source        = "../../terraform/ecs_script_task"
  task_name     = "xml_to_json_converter"
  app_uri       = "${module.ecr_repository_xml_to_json_converter.repository_url}:${var.release_ids["xml_to_json_converter"]}"
  task_role_arn = "${module.ecs_xml_to_json_converter_iam.task_role_arn}"

  env_vars = [
    "{\"name\": \"BUCKET\", \"value\": \"${var.bucket_miro_data_id}\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}

module "ecs_xml_to_json_converter_iam" {
  source = "../../terraform/ecs_iam"
  name   = "xml_to_json_converter"
}

module "ecr_repository_xml_to_json_converter" {
  source = "../../terraform/ecr"
  name   = "xml_to_json_converter"
}
