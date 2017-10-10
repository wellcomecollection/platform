# Lambda for restarting applications when their config changes

module "lambda_update_task_for_config_change" {
  source      = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"
  name        = "update_task_for_config_change"
  description = "Trigger a task definition change and restart on config change."

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/update_task_for_config_change.zip"
}

resource "aws_lambda_permission" "allow_lambda" {
  statement_id  = "AllowExecutionFromS3Bucket_${module.lambda_update_task_for_config_change.function_name}"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_update_task_for_config_change.arn}"
  principal     = "s3.amazonaws.com"
  source_arn    = "${var.bucket_infra_arn}"
}
