module "lambda_gatling_to_cloudwatch" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  s3_key = "lambdas/monitoring/gatling_to_cloudwatch.zip"

  name        = "gatling_to_cloudwatch"
  description = "Record gatling results as CloudWatch metrics"
  timeout     = 5

  alarm_topic_arn = "${data.terraform_remote_state.lambdas.lambda_error_alarm_arn}"
}

module "trigger_gatling_to_cloudwatch" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.lambda_gatling_to_cloudwatch.function_name}"
  lambda_function_arn  = "${module.lambda_gatling_to_cloudwatch.arn}"
  sns_trigger_arn      = "${module.load_test_results.arn}"
}
