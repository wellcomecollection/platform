resource "aws_iam_role_policy" "lambda_transformer_filter_publish_permissions" {
  role   = "${module.lambda_sierra_bibs_merger_filter.role_name}"
  policy = "${module.sierra_bib_merger_events_topic.publish_policy}"
}
