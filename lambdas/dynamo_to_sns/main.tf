module "lambda_dynamo_to_sns" {
  source = "../../terraform/lambda"

  name        = "dynamo_to_sns"
  description = "Push new images form DynamoDB updates to SNS"
  timeout     = 30
  memory_size = 256

  environment_variables = {
    STREAM_TOPIC_MAP = <<EOF
      {
        "${var.miro_table_stream_arn}": "${var.miro_transformer_topic_arn}"
      }
      EOF
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/dynamo_to_sns.zip"
}

module "trigger_dynamo_to_sns_miro" {
  source = "../../terraform/lambda/trigger_dynamo"

  batch_size = "20"

  stream_arn    = "${var.miro_table_stream_arn}"
  function_arn  = "${module.lambda_dynamo_to_sns.arn}"
  function_role = "${module.lambda_dynamo_to_sns.role_name}"
}
