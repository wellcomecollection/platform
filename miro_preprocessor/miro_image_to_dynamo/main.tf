module "miro_image_to_dynamo_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  s3_key = "lambdas/miro_preprocessor/miro_image_to_dynamo.zip"

  description     = "Push image JSON into DynamoDB"
  name            = "miro_image_to_dynamo"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  timeout = "30"

  environment_variables = {
    TABLE_NAME = "${var.miro_data_table_name}"
  }
}

module "miro_image_to_dynamo" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.miro_image_to_dynamo_lambda.function_name}"
  sns_trigger_arn      = "${var.topic_miro_image_to_dynamo_arn}"
  lambda_function_arn  = "${module.miro_image_to_dynamo_lambda.arn}"
}

resource "aws_iam_role_policy" "miro_image_to_dynamo" {
  name   = "miro_image_to_dynamo_policy"
  role   = "${module.miro_image_to_dynamo_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.allow_miro_data_put.json}"
}

data "aws_iam_policy_document" "allow_miro_data_put" {
  statement {
    actions = [
      "dynamodb:PutItem",
    ]

    resources = [
      "${var.miro_data_table_arn}",
    ]
  }
}
