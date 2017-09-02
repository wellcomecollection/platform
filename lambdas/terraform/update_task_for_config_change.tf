# Lambda for restarting applications when their config changes

module "lambda_update_task_for_config_change" {
  source      = "./lambda"
  name        = "update_task_for_config_change"
  description = "Trigger a task definition change and restart on config change."
  source_dir  = "../src/update_task_for_config_change"

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_application_restart_on_config_change" {
  source               = "./lambda/trigger_s3"
  lambda_function_name = "${module.lambda_update_task_for_config_change.function_name}"
  lambda_function_arn  = "${module.lambda_update_task_for_config_change.arn}"
  s3_bucket_arn        = "${data.terraform_remote_state.platform.bucket_infra_arn}"
  s3_bucket_id         = "${data.terraform_remote_state.platform.bucket_infra_id}"
  filter_prefix        = "config/prod/"
  filter_suffix        = ".ini"
}
