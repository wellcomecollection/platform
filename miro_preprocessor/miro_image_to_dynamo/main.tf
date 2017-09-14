module "miro_image_to_dynamo" {
  source = "../../terraform/lambda"
  source_dir = "${path.module}/target"
  description = "Push image json into DynamoDB"
  name = "miro_image_to_dynamo"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  environment_variables = {
    MIRO_S3_BUCKET = "${var.s3_bucket_name}"
    TABLE_NAME = "${var.miro_data_table_name}"
  }
}

module "miro_image_to_dynamo" {
  source = "../../terraform/lambda/trigger_sns"
  lambda_function_name = "${module.miro_image_to_dynamo.function_name}"
  sns_trigger_arn = "${var.miro_image_to_dynamo_topic_arn}"
  lambda_function_arn = "${module.miro_image_to_dynamo.arn}"
}

resource "aws_iam_role_policy" "miro_image_to_dynamo" {
  name   = "miro_image_to_dynamo_policy"
  role   = "${module.miro_image_to_dynamo.role_name}"
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

  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${var.s3_bucket_arn}",
    ]
  }
}