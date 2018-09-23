resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  role   = "${module.transformer.task_role_name}"
  policy = "${var.transformed_works_topic_publish_policy}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_vhs" {
  role   = "${module.transformer.task_role_name}"
  policy = "${var.vhs_read_policy}"
}

resource "aws_iam_role_policy" "transformer_task_cloudwatch_metric" {
  role   = "${module.transformer.task_role_name}"
  policy = "${var.allow_cloudwatch_push_metrics_json}"
}

resource "aws_iam_role_policy" "transformer_s3_messages" {
  role   = "${module.transformer.task_role_name}"
  policy = "${var.allow_s3_messages_put_json}"
}
