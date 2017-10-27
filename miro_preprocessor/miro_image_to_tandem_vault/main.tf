module "miro_image_to_tandem_vault_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  s3_key = "lambdas/miro_preprocessor/miro_image_to_dynamo.zip"

  description     = "Push image JSON into DynamoDB"
  name            = "miro_image_to_dynamo"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    TANDEM_VAULT_API_KEY = "${var.tandem_vault_api_key}"
    TANDEM_VAULT_API_URL = "${var.tandem_vault_api_url}"
    IMAGE_SRC_BUCKET = "${var.bucket_source_asset_name}"
  }
}

module "miro_image_to_tandem_vault_trigger" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_image_to_tandem_vault_lambda.function_name}"
  sns_trigger_arn      = "${var.topic_miro_image_to_tandem_vault_arn}"
  lambda_function_arn  = "${module.miro_image_to_tandem_vault_lambda.arn}"
}

resource "aws_iam_role_policy" "miro_image_to_tandem_vault_lambda" {
  role   = "${module.miro_image_to_tandem_vault_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_get.json}"
}

data "aws_iam_policy_document" "allow_s3_get" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${var.bucket_source_asset_arn}/*",
    ]
  }
}