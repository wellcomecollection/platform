module "lambda_queue_watcher" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/queue_watcher.zip"

  name        = "queue_watcher"
  description = "Post custom notification for queue size on all queues"
  timeout     = 15

  environment_variables = {}

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  log_retention_in_days = 30
}

module "trigger_post_to_slack_dlqs_not_empty" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"

  lambda_function_name = "${module.lambda_queue_watcher.function_name}"
  lambda_function_arn  = "${module.lambda_queue_watcher.arn}"

  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.every_minute.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.every_minute.name}"
}

resource "aws_cloudwatch_event_rule" "every_minute" {
  name                = "every_minute"
  schedule_expression = "cron(* * * * * *)"
}
