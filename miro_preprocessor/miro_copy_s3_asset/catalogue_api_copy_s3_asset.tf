module "miro_copy_s3_catalogue_derivative_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  s3_key = "lambdas/miro_preprocessor/miro_copy_s3_asset.zip"

  description     = "Copy licensed miro derivatives to Loris s3 bucket"
  name            = "miro_copy_s3_catalogue_derivative"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    "S3_SOURCE_BUCKET"      = "${var.bucket_miro_images_sync_name}"
    "S3_DESTINATION_BUCKET" = "${var.bucket_miro_images_public_name}"
    "TOPIC_ARN"             = "${var.topic_miro_image_to_dynamo_arn}"
  }

  timeout = "30"
}

module "miro_copy_s3_catalogue_derivative_trigger" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_copy_s3_catalogue_derivative_lambda.function_name}"
  sns_trigger_arn      = "${var.topic_miro_copy_s3_catalogue_assets_arn}"
  lambda_function_arn  = "${module.miro_copy_s3_catalogue_derivative_lambda.arn}"
}

resource "aws_iam_role_policy" "miro_copy_s3_catalogue_derivative" {
  name   = "miro_copy_s3_catalogue_derivative"
  role   = "${module.miro_copy_s3_catalogue_derivative_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_catalogue_derivative_copy.json}"
}

data "aws_iam_policy_document" "allow_s3_catalogue_derivative_copy" {
  statement {
    actions = [
      "s3:GetObject",
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
    ]

    resources = [
      "${var.bucket_miro_images_public_arn}/*",
    ]
  }
}
