resource "aws_iam_role_policy" "allow_cloudwatch_metrics" {
  role   = "${module.service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "allow_vhs_read" {
  role   = "${module.service.task_role_name}"
  policy = "${data.aws_iam_policy_document.vhs_read_policy.json}"
}

resource "aws_iam_role_policy" "allow_publish_to_sns" {
  role   = "${module.service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sns_publish_policy.json}"
}
