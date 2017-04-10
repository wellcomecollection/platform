module "ecs_services_iam" {
  source = "./ecs_iam"
  name   = "services"
}

module "ecs_calm_adapter_iam" {
  source = "./ecs_iam"
  name   = "calm_adapter"
}

module "ecs_transformer_iam" {
  source = "./ecs_iam"
  name   = "transformer"
}

module "ecs_id_minter_iam" {
  source = "./ecs_iam"
  name   = "id_minter"
}

module "ecs_ingestor_iam" {
  source = "./ecs_iam"
  name   = "ingestor"
}

module "ecs_api_iam" {
  source = "./ecs_iam"
  name   = "api"
}

module "ecs_tools_iam" {
  source = "./ecs_iam"
  name   = "tools"
}

resource "aws_iam_role_policy" "ecs_jenkins_task" {
  name = "ecs_task_jenkins_policy"
  role = "${module.ecs_tools_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.allow_everything.json}"
}

data "aws_iam_policy_document" "allow_everything" {
  statement {
    actions = [
      "*",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "ecs_ingestor_task" {
  name = "ecs_task_ingestor_policy"
  role = "${module.ecs_ingestor_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.read_ingestor_q.json}"
}

data "aws_iam_policy_document" "read_ingestor_q" {
  statement {
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
    ]

    resources = [
      "${module.ingest_queue.q_arn}",
    ]
  }
}

/** Allows the adapter app to write to the CalmData table. */
resource "aws_iam_role_policy" "ecs_calm_adapter_task" {
  name = "ecs_task_calm_adapter_policy"
  role = "${module.ecs_calm_adapter_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.allow_all_calm_db.json}"
}

/** Allows write access to the CalmData table in DynamoDB. */
data "aws_iam_policy_document" "allow_all_calm_db" {
  statement {
    actions = [
      "dynamodb:DescribeTable",
      "dynamodb:PutItem",
      "dynamodb:UpdateTable",
    ]

    resources = [
      "${aws_dynamodb_table.calm-dynamodb-table.arn}",
    ]
  }
}

resource "aws_iam_role_policy" "ecs_transformer_task_dynamo" {
  name = "ecs_task_transformer_task_dynamo_policy"
  role = "${module.ecs_transformer_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.dynamodb_allow_all.json}"
}

data "aws_iam_policy_document" "dynamodb_allow_all" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "*",
    ]
  }
}

/** Allows the transformer app to publish to the ingest topic. */
resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  name = "ecs_task_task_sns_policy"
  role = "${module.ecs_transformer_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.publish_to_id_minter_sns.json}"
}

/** Allows publishing to the ingest topic. */
data "aws_iam_policy_document" "publish_to_id_minter_sns" {
  statement {
    actions = [
      "SNS:Publish",
    ]

    resources = [
      "${aws_sns_topic.id_minter_topic.arn}",
    ]
  }
}

/** Allows the transformer app to publish to the ingest topic. */
resource "aws_iam_role_policy" "ecs_id_minter_task_sns" {
  name = "ecs_task_task_sns_policy"
  role = "${module.ecs_id_minter_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.publish_to_ingest_sns.json}"
}

/** Allows publishing to the ingest topic. */
data "aws_iam_policy_document" "publish_to_ingest_sns" {
  statement {
    actions = [
      "SNS:Publish",
    ]

    resources = [
      "${aws_sns_topic.ingest_topic.arn}",
    ]
  }
}

resource "aws_iam_role_policy" "ecs_transformer_task_kinesis_stream" {
  name = "ecs_task_kinesis_stream_policy"
  role = "${module.ecs_transformer_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.read_calm_kinesis_stream.json}"
}

data "aws_iam_policy_document" "read_calm_kinesis_stream" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.calm-dynamodb-table.stream_arn}",
    ]
  }
}

/** Allows publishing to the service scheduler SNS topic. */
data "aws_iam_policy_document" "publish_to_sns" {
  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      "${aws_sns_topic.service_scheduler_topic.arn}",
    ]
  }
}

/** Allow the "Publish to SNS" Lambda to publish to the service
  * scheduler topic.
  */
resource "aws_iam_role_policy" "publish_to_sns_lambda_policy" {
  name   = "publish_to_sns_policy"
  role   = "${module.publish_to_sns_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_sns.json}"
}

/** Allow the Calm adapter to publish to the service scheduler topic.
  * This is used when it turns itself off at the end of a run.
  */
resource "aws_iam_role_policy" "ecs_calm_adapter_publish_to_sns" {
  name   = "publish_to_sns_policy"
  role   = "${module.ecs_calm_adapter_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_sns.json}"
}
