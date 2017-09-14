# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_xml_to_json_run_task" {
  source     = "../../terraform/lambda"
  source_dir = "${path.module}/target"

  name        = "xml_to_json_run_task"
  description = "Run the Miro XML to JSON ECS task"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
}

module "trigger_application_restart_on_config_change" {
  source = "../../terraform/lambda/trigger_s3"

  lambda_function_name = "${module.lambda_xml_to_json_run_task.function_name}"
  lambda_function_arn  = "${module.lambda_xml_to_json_run_task.arn}"
  s3_bucket_arn        = "${var.bucket_miro_data_arn}"
  s3_bucket_id         = "${var.bucket_miro_data_id}"
  filter_prefix        = "xml"
}
