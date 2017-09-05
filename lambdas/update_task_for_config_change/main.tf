# Lambda for restarting applications when their config changes

module "lambda_update_task_for_config_change" {
  source      = "../../terraform/lambda"
  name        = "update_task_for_config_change"
  description = "Trigger a task definition change and restart on config change."
  source_dir  = "${path.module}/target"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_application_restart_on_config_change" {
  source = "../../terraform/lambda/trigger_s3"

  lambda_function_name = "${module.lambda_update_task_for_config_change.function_name}"
  lambda_function_arn  = "${module.lambda_update_task_for_config_change.arn}"
  s3_bucket_arn        = "${var.bucket_infra_arn}"
  s3_bucket_id         = "${var.bucket_infra_id}"
  filter_prefix        = "config/prod/"
  filter_suffix        = ".ini"
}
