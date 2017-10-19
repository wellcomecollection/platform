module "lambda_dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "dynamo_to_sns"
  description = "Push new images form DynamoDB updates to SNS"
  timeout     = 30
  memory_size = 1024

  environment_variables = {
    STREAM_TOPIC_MAP = <<EOF
      {
        "${var.miro_table_stream_arn}": "${var.miro_transformer_topic_arn}"
      }
      EOF
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/shared_infra/dynamo_to_sns.zip"
}

module "trigger_dynamo_to_sns_miro" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_dynamo?ref=v1.0.0"

  batch_size = "50"

  stream_arn    = "${var.miro_table_stream_arn}"
  function_arn  = "${module.lambda_dynamo_to_sns.arn}"
  function_role = "${module.lambda_dynamo_to_sns.role_name}"
}
