module "miro_copy_s3_asset_lambda" {
  source          = "../../terraform/lambda"
  source_dir      = "${path.module}/target"
  description     = "Copy miro images to another s3 bucket"
  name            = "miro_copy_s3_asset"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    "S3_SOURCE_BUCKET"      = "${var.bucket_miro_images_sync_name}"
    "S3_DESTINATION_BUCKET" = "${var.bucket_miro_images_public_name}"
    "TOPIC_ARN"             = "${var.topic_miro_image_to_dynamo_arn}"
  }

  timeout = "30"
}

module "miro_copy_s3_asset_trigger" {
  source               = "../../terraform/lambda/trigger_sns"
  lambda_function_name = "${module.miro_copy_s3_asset_lambda.function_name}"
  sns_trigger_arn      = "${var.topic_miro_copy_s3_asset_arn}"
  lambda_function_arn  = "${module.miro_copy_s3_asset_lambda.arn}"
}

resource "aws_iam_role_policy" "miro_copy_s3_asset" {
  name   = "miro_copy_s3_asset_policy"
  role   = "${module.miro_copy_s3_asset_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_copy.json}"
}

data "aws_iam_policy_document" "allow_s3_copy" {
  statement {
    actions = [
      "s3:GetObject",
      "s3:ListBucket",
    ]

    resources = [
      "${var.bucket_miro_images_sync_arn}/fullsize/*",
    ]
  }

  statement {
    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "${var.bucket_miro_images_sync_arn}",
      "${var.bucket_miro_images_public_arn}",
    ]
  }

  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObject",
      "s3:ListBucket",
    ]

    resources = [
      "${var.bucket_miro_images_public_arn}/*",
    ]
  }
}
