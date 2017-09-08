# Add your resorces here!
module "xml_to_json_converter" {
  source        = "../terraform/ecs_script_task"
  task_name     = "xml_to_json_converter"
  app_uri       = "${module.ecr_repository_xml_to_json_converter.repository_url}:${var.release_ids["xml_to_json_converter"]}"
  task_role_arn = "${module.ecs_xml_to_json_converter_iam.task_role_arn}"

  # This script has to load the XML files into memory, so make sure it
  # has plenty of overhead.
  memory = 2000

  env_vars = [
    "{\"name\": \"BUCKET\", \"value\": \"${data.terraform_remote_state.platform.bucket_miro_data_id}\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}

module "ecs_xml_to_json_converter_iam" {
  source = "../terraform/ecs_iam"
  name   = "xml_to_json_converter"
}

module "ecr_repository_xml_to_json_converter" {
  source = "../terraform/ecr"
  name   = "xml_to_json_converter"
}

resource "aws_iam_role_policy" "xml_to_json_converter_read_from_s3" {
  role   = "${module.ecs_xml_to_json_converter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_read_miro_data.json}"
}

data "aws_iam_policy_document" "s3_read_miro_data" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
      "s3:Put*",
    ]

    resources = [
      "${data.terraform_remote_state.platform.bucket_miro_data_arn}",
    ]
  }
}