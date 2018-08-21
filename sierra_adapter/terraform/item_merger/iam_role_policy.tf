resource "aws_iam_role_policy" "vhs_full_access_policy" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${var.vhs_full_access_policy}"
}

resource "aws_iam_role_policy" "allow_read_from_updates_q" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${module.updates_queue.read_policy}"
}

resource "aws_iam_role_policy" "push_cloudwatch_metric" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "allow_items_bucket_reading" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_items_bucket_reading.json}"
}

resource "aws_iam_role_policy" "publish_to_sns_topic" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${module.sierra_item_merger_results.publish_policy}"
}
