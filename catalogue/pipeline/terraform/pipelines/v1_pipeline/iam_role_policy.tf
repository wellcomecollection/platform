# Role policies for the Elasticsearch ingestor

resource "aws_iam_role_policy" "ecs_ingestor_task_cloudwatch_metric" {
  role   = "${module.ingestor.task_role_name}"
  policy = "${var.allow_cloudwatch_push_metrics_json}"
}

resource "aws_iam_role_policy" "ingestor_s3_messages_get" {
  role   = "${module.ingestor.task_role_name}"
  policy = "${var.allow_s3_messages_get_json}"
}

# Role policies for the ID minter

resource "aws_iam_role_policy" "ecs_id_minter_task_sns" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${module.es_ingest_topic.publish_policy}"
}

resource "aws_iam_role_policy" "id_minter_cloudwatch" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${var.allow_cloudwatch_push_metrics_json}"
}

resource "aws_iam_role_policy" "id_minter_s3_messages_get" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${var.allow_s3_messages_get_json}"
}

resource "aws_iam_role_policy" "id_minter_s3_messages_put" {
  role   = "${module.id_minter.task_role_name}"
  policy = "${var.allow_s3_messages_put_json}"
}
