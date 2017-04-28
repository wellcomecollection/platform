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

/* TODO: Scope this policy document more tightly */
data "aws_iam_policy_document" "allow_dynamodb_all" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "allow_calm_db_all" {
  statement {
    actions = [
      "dynamodb:DescribeTable",
      "dynamodb:PutItem",
      "dynamodb:UpdateTable",
    ]

    resources = [
      "${aws_dynamodb_table.calm_table.arn}",
    ]
  }
}

data "aws_iam_policy_document" "allow_cloudwatch_push_metrics" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "read_calm_kinesis_stream" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.calm_table.stream_arn}",
    ]
  }
}

data "aws_iam_policy_document" "read_ingestor_q" {
  statement {
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
    ]

    resources = [
      "${module.ingest_queue.arn}",
    ]
  }
}

data "aws_iam_policy_document" "read_id_minter_q" {
  statement {
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
    ]

    resources = [
      "${module.id_minter_queue.arn}",
    ]
  }
}

data "aws_iam_policy_document" "read_write_dynamo_identifiers_table" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.identifiers.arn}",
      "${aws_dynamodb_table.identifiers.arn}/index/*",
    ]
  }
}

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

data "aws_iam_policy_document" "publish_to_ingest_sns" {
  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      "${aws_sns_topic.ingest_topic.arn}",
    ]
  }
}

data "aws_iam_policy_document" "publish_to_scheduler_sns" {
  statement {
    actions = [
      "sns:Publish",
    ]

    resources = [
      "${aws_sns_topic.service_scheduler_topic.arn}",
    ]
  }
}

data "aws_iam_policy_document" "update_ecs_service_size" {
  statement {
    actions = [
      "ecs:UpdateService",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "stop_running_tasks" {
  statement {
    actions = [
      "ecs:ListServices",
      "ecs:ListClusters",
      "ecs:ListTasks",
      "ecs:StopTask",
    ]

    resources = [
      "*",
    ]
  }
}
