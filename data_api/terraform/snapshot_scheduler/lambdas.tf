module "snapshot_scheduler_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name = "snapshot_scheduler"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/data_api/snapshot_scheduler.zip"

  description     = "Send snapshot schedules to an SNS topic"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  timeout         = 10

  environment_variables = {
    TOPIC_ARN = "${module.scheduler_topic.arn}"

    PUBLIC_BUCKET_NAME  = "${var.public_bucket_name}"

    PUBLIC_OBJECT_KEY_V1 = "${var.public_object_key_v1}"
    PUBLIC_OBJECT_KEY_V2 = "${var.public_object_key_v2}"
  }
}

module "trigger_snapshot_scheduler_lambda" {
  source                  = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"
  lambda_function_name    = "${module.snapshot_scheduler_lambda.function_name}"
  lambda_function_arn     = "${module.snapshot_scheduler_lambda.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.snapshot_scheduler_rule.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.snapshot_scheduler_rule.id}"
}
