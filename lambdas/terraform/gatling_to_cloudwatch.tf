# Lambda for recording gatling results as cloudwatch metrics

module "lambda_gatling_to_cloudwatch" {
  source      = "./lambda"
  name        = "gatling_to_cloudwatch"
  description = "Record gatling results as CloudWatch metrics"
  source_dir  = "../src/gatling_to_cloudwatch"

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_gatling_to_cloudwatch" {
  source               = "./lambda/trigger_sns"
  lambda_function_name = "${module.lambda_gatling_to_cloudwatch.function_name}"
  lambda_function_arn  = "${module.lambda_gatling_to_cloudwatch.arn}"
  sns_trigger_arn      = "${data.terraform_remote_state.platform.load_test_results_arn}"
}
