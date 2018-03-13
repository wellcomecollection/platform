resource "aws_iam_role_policy" "allow_s3_bucket_write" {
  role   = "${module.elasticdump.task_role_name}"
  policy = "${module.id_minter_topic.publish_policy}"
}