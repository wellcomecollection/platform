module "gatling_to_cloudwatch" {
  source = "gatling_to_cloudwatch"

  load_test_results_arn              = "${module.load_test_results.arn}"
  allow_cloudwatch_push_metrics_json = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"

  infra_bucket = "${var.infra_bucket}"
}
