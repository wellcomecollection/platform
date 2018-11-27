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
      "arn:aws:s3:::${local.workflow_bucket_name}/*",
      "arn:aws:s3:::${var.bagger_drop_bucket_name}/*",
      "arn:aws:s3:::${local.ingest_bucket_name}/*",
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
