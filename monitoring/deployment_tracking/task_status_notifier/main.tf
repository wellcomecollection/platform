module "lambda_task_status_notifier" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.4"

  name        = "task_status_notifier"
  description = "Lambda for notifiying changes in task status"

  environment_variables = {
    TASK_STOPPED_TOPIC_ARN = "${module.task_status_change_topic.arn}"
    TASK_STARTED_TOPIC_ARN = "${module.task_status_change_topic.arn}"
    TASK_UPDATED_TOPIC_ARN = "${module.task_status_change_topic.arn}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/deployment_tracking/task_status_notifier.zip"
}

module "trigger_task_status_notifier" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.4"

  lambda_function_name = "${module.lambda_task_status_notifier.function_name}"
  lambda_function_arn  = "${module.lambda_task_status_notifier.arn}"
  sns_trigger_arn      = "${var.sns_trigger_arn}"
}
