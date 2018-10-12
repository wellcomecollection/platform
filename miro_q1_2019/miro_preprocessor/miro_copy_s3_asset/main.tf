module "miro_copy_s3_asset_lambda" {
  source      = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.3"
  s3_key      = "lambdas/miro_preprocessor/miro_copy_s3_asset/miro_copy_s3_${local.lambda_s3_type}_asset.zip"
  module_name = "miro_copy_s3_${local.lambda_s3_type}_asset"

  description     = "${var.lambda_description}"
  name            = "${var.lambda_name}"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    "S3_SOURCE_BUCKET"      = "${var.bucket_source_asset_name}"
    "S3_DESTINATION_BUCKET" = "${var.bucket_destination_name}"
    "S3_DESTINATION_PREFIX" = "${var.destination_key_prefix}"
    "TOPIC_ARN"             = "${var.topic_forward_sns_message_arn}"
  }

  timeout = "120"
}

module "miro_copy_s3_asset_trigger" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_copy_s3_asset_lambda.function_name}"
  sns_trigger_arn      = "${var.topic_miro_copy_s3_asset_arn}"
  lambda_function_arn  = "${module.miro_copy_s3_asset_lambda.arn}"
}

resource "aws_iam_role_policy" "miro_copy_s3_asset" {
  name   = "${var.lambda_name}_policy"
  role   = "${module.miro_copy_s3_asset_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_copy.json}"
}

data "aws_iam_policy_document" "allow_s3_copy" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${var.bucket_source_asset_arn}/${local.source_prefix}*",
    ]
  }

  statement {
    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "${var.bucket_source_asset_arn}",
      "${var.bucket_destination_asset_arn}",
    ]
  }

  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObject",
    ]

    resources = [
      "${var.bucket_destination_asset_arn}/${var.destination_key_prefix}*",
    ]
  }
}

locals {
  source_prefix  = "${var.is_master_asset == "true" ? "Wellcome_Images_Archive/": "fullsize/"}"
  lambda_s3_type = "${var.is_master_asset == "true" ? "master": "derivative"}"
}
