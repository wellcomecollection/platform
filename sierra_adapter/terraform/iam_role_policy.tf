resource "aws_iam_role_policy" "lambda_transformer_filter_bibs_publish_permissions" {
  role   = "${module.lambda_sierra_bibs_merger_filter.role_name}"
  policy = "${module.sierra_bib_merger_events_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_transformer_filter_items_publish_permissions" {
  role   = "${module.lambda_sierra_items_merger_filter.role_name}"
  policy = "${module.sierra_items_merger_events_topic.publish_policy}"
}
