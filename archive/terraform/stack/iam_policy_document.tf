data "aws_iam_policy_document" "read_from_registrar_queue" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      "${module.bags_queue.arn}",
    ]
  }
}

data "aws_iam_policy_document" "read_from_bagger_queue" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueUrl",
    ]

    resources = [
      "${module.bagger_queue.arn}",
    ]
  }
}

data "aws_iam_policy_document" "read_from_archivist_queue" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      "${module.archivist_queue.arn}",
    ]
  }
}

data "aws_iam_policy_document" "read_from_progress_async_queue" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      "${module.ingests_async_queue.arn}",
    ]
  }
}

data "aws_iam_policy_document" "read_from_notifier_queue" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      "${module.notifier_queue.arn}",
    ]
  }
}

data "aws_iam_policy_document" "archive_progress_table_read_write_policy" {
  statement {
    actions = [
      "dynamodb:UpdateItem",
      "dynamodb:PutItem",
      "dynamodb:GetItem",
      "dynamodb:DeleteItem",
    ]

    resources = [
      "${aws_dynamodb_table.archive_progress_table.arn}",
    ]
  }

  statement {
    actions = [
      "dynamodb:Query",
    ]

    resources = [
      "${aws_dynamodb_table.archive_progress_table.arn}/index/*",
    ]
  }
}
