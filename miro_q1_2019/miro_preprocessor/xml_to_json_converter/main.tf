module "xml_to_json_converter" {
  source        = "git::https://github.com/wellcometrust/terraform.git//ecs_script_task?ref=v1.0.0"
  task_name     = "xml_to_json_converter"
  app_uri       = "${module.ecr_repository_xml_to_json_converter.repository_url}:${var.release_ids["xml_to_json_converter"]}"
  task_role_arn = "${module.ecs_xml_to_json_converter_iam.task_role_arn}"

  env_vars = [
    "{\"name\": \"BUCKET\", \"value\": \"${var.bucket_miro_data_id}\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}

module "ecs_xml_to_json_converter_iam" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "xml_to_json_converter"
}

module "ecr_repository_xml_to_json_converter" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecr?ref=v1.0.0"
  name   = "xml_to_json_converter"
}
