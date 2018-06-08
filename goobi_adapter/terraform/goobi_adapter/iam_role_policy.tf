resource "aws_iam_role_policy" "allow_s3_access" {
  role   = "${module.goobi_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_access.json}"
}

resource "aws_iam_role_policy" "allow_read_from_goobi_mets_q" {
  role   = "${module.goobi_reader_service.task_role_name}"
  policy = "${module.goobi_mets_queue.read_policy}"
}

resource "aws_iam_role_policy" "push_cloudwatch_metric" {
  role   = "${module.goobi_reader_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "goobi_task_vhs" {
  role   = "${module.goobi_reader_service.task_role_name}"
  policy = "${var.vhs_goobi_full_access_policy}"
}
