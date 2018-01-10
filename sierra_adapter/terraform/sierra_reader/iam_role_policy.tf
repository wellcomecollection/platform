resource "aws_iam_role_policy" "allow_s3_access" {
  role   = "${module.sierra_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_access.json}"
}

resource "aws_iam_role_policy" "allow_read_from_windows_q" {
  role   = "${module.sierra_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_windows_q.json}"
}
