module "lambda_gatling_to_cloudwatch" {
  source     = "../../terraform/lambda"
  source_dir = "${path.module}/target"

  name        = "gatling_to_cloudwatch"
  description = "Record gatling results as CloudWatch metrics"
  timeout     = 5

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_gatling_to_cloudwatch" {
  source               = "../../terraform/lambda/trigger_sns"
  lambda_function_name = "${module.lambda_gatling_to_cloudwatch.function_name}"
  lambda_function_arn  = "${module.lambda_gatling_to_cloudwatch.arn}"
  sns_trigger_arn      = "${var.load_test_results_arn}"
}
