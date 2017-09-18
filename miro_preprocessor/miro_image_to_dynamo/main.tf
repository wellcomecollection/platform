module "miro_image_to_dynamo_lambda" {
  source          = "../../terraform/lambda"
  source_dir      = "${path.module}/target"
  description     = "Push image json into DynamoDB"
  name            = "miro_image_to_dynamo"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    TABLE_NAME     = "${var.miro_data_table_name}"
  }
}

module "miro_image_to_dynamo" {
  source               = "../../terraform/lambda/trigger_sns"
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
