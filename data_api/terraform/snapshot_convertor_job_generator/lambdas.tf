module "snapshot_convertor_job_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name = "snapshot_convertor_job_generator"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/data_api/snapshot_convertor_job_generator.zip"

  description     = "Create snapshot_convertor jobs from S3 events."
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  timeout         = 10

  environment_variables = {
    "TOPIC_ARN"          = "${module.snapshot_convertor_topic.arn}"
    "TARGET_BUCKET_NAME" = "${var.target_bucket_name}"
    "TARGET_OBJECT_KEY"  = "${var.target_object_key}"
  }
}
