# Add your resorces here!
module "miro_preprocessor" {
  source        = "../terraform/ecs_script_task"
  task_name     = "miro_preprocessor"
  app_uri       = "${module.ecr_repository_miro_preprocessor.repository_url}:${var.release_ids["miro_adapter"]}"
  task_role_arn = "${module.ecs_miro_preprocessor_iam.task_role_arn}"

  # This script has to load the XML files into memory, so make sure it
  # has plenty of overhead.
  memory = 2000

  env_vars = [
    "{\"name\": \"BUCKET\", \"value\": \"${data.terraform_remote_state.platform.bucket_miro_data_id}\"}",
    "{\"name\": \"AWS_DEFAULT_REGION\", \"value\": \"${var.aws_region}\"}",
  ]
}

module "ecs_miro_preprocessor_iam" {
  source = "../terraform/ecs_iam"
  name   = "miro_preprocessor"
}

module "ecr_repository_miro_preprocessor" {
  source = "../terraform/ecr"
  name   = "miro_preprocessor"
}

resource "aws_iam_role_policy" "miro_preprocessor_read_from_s3" {
  role   = "${module.ecs_miro_preprocessor_iam.task_role_name}"
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