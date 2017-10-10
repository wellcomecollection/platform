module "lambda_update_dynamo_capacity" {
  source      = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  name        = "update_dynamo_capacity"
  description = "Update the capacity of a DynamoDB table"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/update_dynamo_capacity.zip"
}

module "trigger_update_dynamo_capacity" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.lambda_update_dynamo_capacity.function_name}"
  lambda_function_arn  = "${module.lambda_update_dynamo_capacity.arn}"
  sns_trigger_arn      = "${var.dynamo_capacity_topic_arn}"
}
