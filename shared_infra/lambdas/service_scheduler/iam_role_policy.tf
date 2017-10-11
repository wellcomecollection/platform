resource "aws_iam_role_policy" "lambda_service_scheduler_sns" {
  name   = "lambda_service_scheduler_sns_policy"
  role   = "${module.lambda_service_scheduler.role_name}"
  policy = "${var.service_scheduler_topic_publish_policy}"
}
