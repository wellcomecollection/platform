data "aws_iam_policy_document" "archive_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      # Allow archivist to read bagger drop bucket
      "arn:aws:s3:::${var.bagger_drop_bucket_name}/*",

      # Allow archivist to read our archive ingest bucket
      "arn:aws:s3:::${local.archive_bucket_name}",

      "arn:aws:s3:::${local.archive_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "archive_store" {
  statement {
    actions = [
      "s3:PutObject*",
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${local.archive_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "ingest_get" {
  statement {
    actions = [
      "s3:GetObject*",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${local.ingest_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "ingest_get_bagger" {
  statement {
    actions = [
      "s3:GetObject*",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${var.bagger_drop_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "ingest_workflow_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::wellcomecollection-workflow-export-bagit/*",
    ]
  }
}

data "aws_iam_policy_document" "archive_dlcs_get" {
  statement {
    effect = "Allow"

    principals {
      type = "AWS"

      identifiers = [
        "arn:aws:iam::653428163053:user/echo-fs",
        "arn:aws:iam::653428163053:user/api",
      ]
    }

    actions = [
      "s3:GetObject",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${local.archive_bucket_name}",
      "arn:aws:s3:::${local.archive_bucket_name}/*",
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

# progress_async

data "aws_iam_policy_document" "read_from_progress_async_queue" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      "${module.progress_async_queue.arn}",
    ]
  }
}

# Notifier

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

# Bagger

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

data "aws_iam_policy_document" "bagger_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${var.bagger_mets_bucket_name}",
      "arn:aws:s3:::${var.bagger_mets_bucket_name}/*",
    ]
  }
}

data "aws_iam_policy_document" "bagger_store" {
  statement {
    actions = [
      "s3:PutObject*",
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${local.ingest_bucket_name}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name_mets_only}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name_errors}/*",
    ]
  }
}

data "aws_iam_policy_document" "bagger_get_dlcs" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${var.bagger_dlcs_source_bucket}",
      "arn:aws:s3:::${var.bagger_dlcs_source_bucket}/*",
    ]
  }
}

data "aws_iam_policy_document" "bagger_get_preservica" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${var.bagger_current_preservation_bucket}",
      "arn:aws:s3:::${var.bagger_current_preservation_bucket}/*",
    ]
  }
}

data "aws_iam_policy_document" "read_from_registrar_queue" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      "${module.registrar_queue.arn}",
    ]
  }
}
