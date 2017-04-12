# Role policies for Jenkins

resource "aws_iam_role_policy" "ecs_jenkins_task" {
  name   = "ecs_task_jenkins_policy"
  role   = "${module.ecs_tools_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_everything.json}"
}

# Role policies for the Elasticsearch ingestor

resource "aws_iam_role_policy" "ecs_ingestor_task_read_ingestor_q" {
  name   = "ecs_task_ingestor_policy"
  role   = "${module.ecs_ingestor_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_ingestor_q.json}"
}

# Role policies for the Calm adapter

resource "aws_iam_role_policy" "ecs_calm_adapter_task" {
  name   = "ecs_task_calm_adapter_policy"
  role   = "${module.ecs_calm_adapter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_calm_db_all.json}"
}

resource "aws_iam_role_policy" "ecs_calm_adapter_service_scheduler_sns" {
  name   = "ecs_task_calm_service_scheduler_sns_policy"
  role   = "${module.ecs_calm_adapter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_scheduler_sns.json}"
}

# Role policies for the transformer

resource "aws_iam_role_policy" "ecs_transformer_task_dynamo" {
  name   = "ecs_task_transformer_task_dynamo_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_dynamodb_all.json}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  name   = "ecs_task_task_sns_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_id_minter_sns.json}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_kinesis_stream" {
  name   = "ecs_task_kinesis_stream_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_calm_kinesis_stream.json}"
}

resource "aws_iam_role_policy" "ecs_transformer_task_cloudwatch_metric" {
  name   = "ecs_task_cloudwatch_metric_policy"
  role   = "${module.ecs_transformer_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

# Role policies for the ID minter

resource "aws_iam_role_policy" "ecs_id_minter_task_sns" {
  name   = "ecs_task_task_sns_policy"
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_ingest_sns.json}"
}

resource "aws_iam_role_policy" "ecs_id_minter_task_read_id_minter_q" {
  name   = "ecs_task_id_minter_policy"
  role   = "${module.ecs_id_minter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_id_minter_q.json}"
}

# Role policies for the Publish to SNS Lambda

resource "aws_iam_role_policy" "lambda_service_scheduler_sns" {
  name   = "lambda_service_scheduler_sns_policy"
  role   = "${module.lambda_service_scheduler.role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_scheduler_sns.json}"
}
