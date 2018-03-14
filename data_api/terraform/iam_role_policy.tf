# Role policies for the snapshot_convertor

resource "aws_iam_role_policy" "ecs_snapshot_convertor_task_sns" {
  role   = "${module.snapshot_convertor.task_role_name}"
  policy = "${module.snapshot_conversion_complete_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_snapshot_convertor_task_s3_public" {
  role   = "${module.snapshot_convertor.task_role_name}"
  policy = "${data.aws_iam_policy_document.public_data_bucket_full_access_policy.json}"
}

resource "aws_iam_role_policy" "ecs_snapshot_convertor_task_s3_private" {
  role   = "${module.snapshot_convertor.task_role_name}"
  policy = "${data.aws_iam_policy_document.private_data_bucket_full_access_policy.json}"
}

resource "aws_iam_role_policy" "snapshot_convertor_cloudwatch" {
  role   = "${module.snapshot_convertor.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for Elasticdump

resource "aws_iam_role_policy" "allow_s3_elasticdump_write" {
  role   = "${module.elasticdump.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_write.json}"
}
