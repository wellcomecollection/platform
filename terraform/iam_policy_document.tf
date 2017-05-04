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

data "aws_iam_policy_document" "travis_permissions" {
  statement {
    actions = [
      "ecr:*",
    ]

    resources = [
      "${aws_ecr_repository.api.arn}",
      "${aws_ecr_repository.transformer.arn}",
      "${aws_ecr_repository.ingestor.arn}",
      "${aws_ecr_repository.id_minter.arn}",
      "${aws_ecr_repository.calm_adapter.arn}",
    ]
  }

  statement {
    actions = [
      "ecr:GetAuthorizationToken",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObject",
    ]

    resources = [
      "${aws_s3_bucket.infra.arn}/releases/*",
    ]
  }
}
