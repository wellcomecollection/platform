resource "aws_iam_role_policy" "allow_s3_bucket_write" {
  role   = "${module.elasticdump.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_write.json}"
}
