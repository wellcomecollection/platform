module "lambda_dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.2.0"

  name        = "dynamo_to_sns_${var.name}"
  module_name = "dynamo_to_sns"
  description = "Push new images form DynamoDB updates to SNS"
  timeout     = 30
  memory_size = 1024

  environment_variables = {
    TOPIC_ARN        = "${var.dst_topic_arn}"
    STREAM_VIEW_TYPE = "${var.stream_view_type}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/shared_infra/dynamo_to_sns.zip"
}

module "trigger_dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_dynamo?ref=v1.0.0"

  batch_size = "${var.batch_size}"

  stream_arn    = "${var.src_stream_arn}"
  function_arn  = "${module.lambda_dynamo_to_sns.arn}"
  function_role = "${module.lambda_dynamo_to_sns.role_name}"
}
