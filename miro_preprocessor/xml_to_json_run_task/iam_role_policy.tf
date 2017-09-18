resource "aws_iam_role_policy" "xml_to_json_run_task_read_from_s3" {
  role   = "${module.lambda_xml_to_json_run_task.role_name}"
  policy = "${var.s3_read_miro_data_json}"
}

resource "aws_iam_role_policy" "lambda_service_scheduler_sns" {
  name   = "lambda_service_scheduler_sns_policy"
  role   = "${module.lambda_xml_to_json_run_task.role_name}"
  policy = "${var.run_ecs_task_topic_publish_policy}"
}
