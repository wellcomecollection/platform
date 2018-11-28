resource "aws_iam_role_policy" "allow_s3_access" {
  role   = "${module.sierra_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_access.json}"
}

resource "aws_iam_role_policy" "allow_read_from_windows_q" {
  role   = "${module.sierra_reader_service.task_role_name}"
  policy = "${module.windows_queue.read_policy}"
}

resource "aws_iam_role_policy" "push_cloudwatch_metric" {
  role   = "${module.sierra_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
