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

data "aws_iam_policy_document" "allow_s3_messages_put" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::${local.messages_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "allow_s3_messages_get" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "arn:aws:s3:::${local.messages_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "graph_table_read_write_policy" {
  statement {
    actions = [
      "dynamodb:UpdateItem",
      "dynamodb:PutItem",
      "dynamodb:BatchGetItem",
      "dynamodb:GetItem",
    ]

    resources = [
      "${aws_dynamodb_table.matcher_graph_table.arn}",
    ]
  }

  statement {
    actions = [
      "dynamodb:Query",
    ]

    resources = [
      "${aws_dynamodb_table.matcher_graph_table.arn}/index/*",
    ]
  }
}

data "aws_iam_policy_document" "lock_table_read_write_policy" {
  statement {
    actions = [
      "dynamodb:UpdateItem",
      "dynamodb:PutItem",
      "dynamodb:GetItem",
      "dynamodb:BatchWriteItem"
    ]

    resources = [
      "${aws_dynamodb_table.matcher_lock_table.arn}",
    ]
  }

  statement {
    actions = [
      "dynamodb:Query",
    ]

    resources = [
      "${aws_dynamodb_table.matcher_lock_table.arn}/index/*",
    ]
  }
}
