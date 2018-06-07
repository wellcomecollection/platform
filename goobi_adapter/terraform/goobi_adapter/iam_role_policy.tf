resource "aws_iam_role_policy" "allow_s3_access" {
  role   = "${module.goobi_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_access.json}"
}

resource "aws_iam_role_policy" "allow_read_from_goobi_items_q" {
  role   = "${module.goobi_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_goobi_items_q.json}"
}

resource "aws_iam_role_policy" "push_cloudwatch_metric" {
  role   = "${module.goobi_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
