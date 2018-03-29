resource "aws_iam_role_policy" "snapshot_scheduler_sns_publish" {
  role   = "${module.snapshot_convertor_job_generator_lambda.role_name}"
  policy = "${module.snapshot_convertor_topic.publish_policy}"
}
