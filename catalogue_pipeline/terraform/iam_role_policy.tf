# Role policies for the transformer

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  role   = "${module.transformer.task_role_name}"
  policy = "${module.id_minter_topic.publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_vhs" {
  role   = "${module.transformer.task_role_name}"
  policy = "${module.versioned-hybrid-store.read_policy}"
}

resource "aws_iam_role_policy" "transformer_task_cloudwatch_metric" {
  role   = "${module.transformer.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the Elasticsearch ingestor

resource "aws_iam_role_policy" "ecs_ingestor_task_cloudwatch_metric" {
  role   = "${module.ingestor.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
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

# Role policies for the shard generator lambda

resource "aws_iam_role_policy" "allow_shard_generator_put_vhs" {
  role   = "${module.shard_generator_lambda.role_name}"
  policy = "${module.versioned-hybrid-store.dynamo_update_policy}"
}
