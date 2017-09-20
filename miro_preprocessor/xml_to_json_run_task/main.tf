# Lambda for publishing ECS service schedules to an SNS topic

module "lambda_xml_to_json_run_task" {
  source     = "../../terraform/lambda"
  source_dir = "${path.module}/target"

  name        = "xml_to_json_run_task"
  description = "Start the Miro XML to JSON ECS task"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    TOPIC_ARN           = "${var.topic_arn}"
    CLUSTER_NAME        = "${var.cluster_name}"
    CONTAINER_NAME      = "${var.container_name}"
    TASK_DEFINITION_ARN = "${var.task_definition_arn}"
  }
}

resource "aws_lambda_permission" "allow_lambda" {
  statement_id  = "AllowExecutionFromS3Bucket_${module.lambda_xml_to_json_run_task.function_name}_${var.bucket_miro_data_id}"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_xml_to_json_run_task.function_name}"
  principal     = "s3.amazonaws.com"
  source_arn    = "${var.bucket_miro_data_arn}"
}
