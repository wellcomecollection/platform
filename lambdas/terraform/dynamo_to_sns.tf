# Lambda for pushing updates to dynamo tables into sqs queues

module "lambda_dynamo_to_sns" {
  source      = "./lambda"
  name        = "dynamo_to_sns"
  description = "Push new images form DynamoDB updates to SNS"
  source_dir  = "../src/dynamo_to_sns"
  timeout     = 30

  environment_variables = {
    STREAM_TOPIC_MAP = <<EOF
      {
        "${data.terraform_remote_state.platform.miro_table_stream_arn}": "${data.terraform_remote_state.platform.transformer_topic_arn}"
      }
      EOF
  }

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_dynamo_to_sns_miro" {
  source        = "./lambda/trigger_dynamo"
  stream_arn    = "${data.terraform_remote_state.platform.miro_table_stream_arn}"
  function_arn  = "${module.lambda_dynamo_to_sns.arn}"
  function_role = "${module.lambda_dynamo_to_sns.role_name}"
}
