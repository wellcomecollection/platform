# Role policies for the Elasticsearch ingestor

resource "aws_iam_role_policy" "ecs_ingestor_task_cloudwatch_metric" {
  role   = "${module.ingestor.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "ingestor_s3_messages_get" {
  role   = "${module.ingestor.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_get.json}"
}

# Role policies for the ID minter

resource "aws_iam_role_policy" "ecs_id_minter_task_sns" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${module.es_ingest_topic.publish_policy}"
}

resource "aws_iam_role_policy" "id_minter_cloudwatch" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "id_minter_s3_messages_get" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_get.json}"
}

resource "aws_iam_role_policy" "id_minter_s3_messages_put" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
}

# Role policies for the Recorder

resource "aws_iam_role_policy" "recorder_cloudwatch" {
  role   = "${module.recorder.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "recorder_s3_messages_get" {
  role   = "${module.recorder.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_get.json}"
}

resource "aws_iam_role_policy" "ecs_recorder_task_vhs" {
  role   = "${module.recorder.task_role_name}"
  policy = "${module.vhs_recorder.full_access_policy}"
}

resource "aws_iam_role_policy" "recorder_task_sns" {
  role   = "${module.recorder.task_role_name}"
  policy = "${module.recorded_works_topic.publish_policy}"
}

# Role policies for the Matcher

resource "aws_iam_role_policy" "matcher_cloudwatch" {
  role   = "${module.matcher.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "matcher_task_vhs" {
  role   = "${module.matcher.task_role_name}"
  policy = "${module.vhs_recorder.read_policy}"
}

resource "aws_iam_role_policy" "matcher_task_sns" {
  role   = "${module.matcher.task_role_name}"
  policy = "${module.matched_works_topic.publish_policy}"
}

resource "aws_iam_role_policy" "matcher_read_write_graph_dynamo" {
  role   = "${module.matcher.task_role_name}"
  policy = "${data.aws_iam_policy_document.graph_table_read_write_policy.json}"
}

resource "aws_iam_role_policy" "matcher_read_write_lock_dynamo" {
  role   = "${module.matcher.task_role_name}"
  policy = "${data.aws_iam_policy_document.lock_table_read_write_policy.json}"
}

# Role policies for the Merger

resource "aws_iam_role_policy" "merger_cloudwatch" {
  role   = "${module.merger.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "merger_task_sns" {
  role   = "${module.merger.task_role_name}"
  policy = "${module.merged_works_topic.publish_policy}"
}

resource "aws_iam_role_policy" "merger_task_read_recorder_vhs" {
  role   = "${module.merger.task_role_name}"
  policy = "${module.vhs_recorder.read_policy}"
}

resource "aws_iam_role_policy" "merger_s3_messages" {
  role   = "${module.merger.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
}
