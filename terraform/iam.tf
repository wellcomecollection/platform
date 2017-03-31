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

resource "aws_iam_role_policy" "ecs_calm_adapter_task" {
  name = "ecs_task_calm_adapter_policy"
  role = "${module.ecs_calm_adapter_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.allow_all_calm_db.json}"
}

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

resource "aws_iam_role_policy" "ecs_transformer_task_sns" {
  name = "ecs_task_jenkins_policy"
  role = "${module.ecs_transformer_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.publish_to_calm_sns.json}"
}

data "aws_iam_policy_document" "publish_to_calm_sns" {
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
  name = "ecs_task_jenkins_policy"
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
