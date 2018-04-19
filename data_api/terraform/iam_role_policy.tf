# Role policies for the snapshot_generator

resource "aws_iam_role_policy" "ecs_snapshot_generator_task_sns" {
  role   = "${module.snapshot_generator.task_role_name}"
  policy = "${module.snapshot_generation_complete_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_snapshot_generator_task_s3_public" {
  role   = "${module.snapshot_generator.task_role_name}"
  policy = "${data.aws_iam_policy_document.public_data_bucket_full_access_policy.json}"
}

resource "aws_iam_role_policy" "ecs_snapshot_generator_task_s3_private" {
  role   = "${module.snapshot_generator.task_role_name}"
  policy = "${data.aws_iam_policy_document.private_data_bucket_full_access_policy.json}"
}

resource "aws_iam_role_policy" "snapshot_generator_cloudwatch" {
  role   = "${module.snapshot_generator.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
