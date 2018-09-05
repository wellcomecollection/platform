# archivist

resource "aws_iam_role_policy" "archivist_task_store_s3" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_store.json}"
}

resource "aws_iam_role_policy" "archivist_task_get_s3" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.ingest_get.json}"
}

resource "aws_iam_role_policy" "archivist_task_sns" {
  role   = "${module.archivist.task_role_name}"
  policy = "${module.registrar_topic.publish_policy}"
}

resource "aws_iam_role_policy" "archivist_task_sqs" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_archivist_queue.json}"
}

resource "aws_iam_role_policy" "archive_bag_vhs" {
  role   = "${module.lambda_archive_bags.role_name}"
  policy = "${module.vhs_archive_manifest.read_policy}"
}

data "aws_iam_policy_document" "archive_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "arn:aws:s3:::${local.archive_bucket_name}",
      "arn:aws:s3:::${local.archive_bucket_name}/*",
    ]
  }
}

resource "aws_iam_role_policy" "archivist_task_archive_progress_table" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# registrar

resource "aws_iam_role_policy" "registrar_task_get_s3" {
  role   = "${module.registrar.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_get.json}"
}

resource "aws_iam_role_policy" "registrar_task_vhs" {
  role   = "${module.registrar.task_role_name}"
  policy = "${module.vhs_archive_manifest.full_access_policy}"
}

resource "aws_iam_role_policy" "registrar_task_sns" {
  role   = "${module.registrar.task_role_name}"
  policy = "${module.registrar_completed_topic.publish_policy}"
}

resource "aws_iam_role_policy" "registrar_task_sqs" {
  role   = "${module.registrar.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_registrar_queue.json}"
}

resource "aws_iam_role_policy" "registrar_task_archive_progress_table" {
  role   = "${module.registrar.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# asset lookup lambda

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

resource "aws_iam_role_policy" "archive_asset_lookup_dynamo_permission" {
  role = "${module.lambda_archive_bags.role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:Query"
      ],
      "Resource": "${data.aws_dynamodb_table.storage_manifest.arn}"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:HeadObject",
        "s3:GetObject"
      ],
      "Resource": "${data.aws_s3_bucket.storage_manifests.arn}/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "${data.aws_s3_bucket.storage_manifests.arn}"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "lambda_archive_ingest_sns" {
  role   = "${module.lambda_archive_ingest.role_name}"
  policy = "${module.archivist_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_archive_ingest_archive_progress_table" {
  role   = "${module.lambda_archive_ingest.role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}
