# Role policies for the transformer

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  role   = "${module.transformer.task_role_name}"
  policy = "${module.transformed_works_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_vhs" {
  role   = "${module.transformer.task_role_name}"
  policy = "${module.vhs_sourcedata.read_policy}"
}

resource "aws_iam_role_policy" "transformer_task_cloudwatch_metric" {
  role   = "${module.transformer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "transformer_s3_messages" {
  role   = "${module.transformer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
}

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