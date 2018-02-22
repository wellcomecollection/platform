module "lambda_gatling_to_cloudwatch" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/load_test/gatling_to_cloudwatch.zip"

  name        = "gatling_to_cloudwatch"
  description = "Record gatling results as CloudWatch metrics"
  timeout     = 5

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_gatling_to_cloudwatch" {
  source               = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"
  lambda_function_name = "${module.lambda_gatling_to_cloudwatch.function_name}"
  lambda_function_arn  = "${module.lambda_gatling_to_cloudwatch.arn}"
  sns_trigger_arn      = "${var.load_test_results_arn}"
}
