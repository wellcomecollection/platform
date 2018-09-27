# Archive API (Flask)

resource "aws_iam_role_policy" "archive_api_task_sns" {
  role   = "${module.api_ecs.task_role_name}"
  policy = "${module.archivist_topic.publish_policy}"
}

resource "aws_iam_role_policy" "archive_api_task_progress_table" {
  role   = "${module.api_ecs.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

resource "aws_iam_role_policy" "archive_api_task_bag_vhs" {
  role   = "${module.api_ecs.task_role_name}"
  policy = "${module.vhs_archive_manifest.read_policy.json}"
}

# Archivist

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

resource "aws_iam_role_policy" "archivist_task_archive_progress_table" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# Registrar

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

# Progress

resource "aws_iam_role_policy" "progress_task_sns" {
  role   = "${module.progress.task_role_name}"
  policy = "${module.caller_topic.publish_policy}"
}

resource "aws_iam_role_policy" "progress_task_sqs" {
  role   = "${module.progress.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_progress_queue.json}"
}

resource "aws_iam_role_policy" "progress_task_archive_progress_table" {
  role   = "${module.progress.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
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

# asset lookup lambda

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

resource "aws_iam_role_policy" "lambda_archive_report_ingest_status_sns" {
  role   = "${module.lambda_archive_report_ingest_status.role_name}"
  policy = "${module.archivist_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_archive_report_ingest_status_archive_progress_table" {
  role   = "${module.lambda_archive_report_ingest_status.role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

resource "aws_iam_role_policy" "lambda_archive_request_ingest_sns" {
  role   = "${module.lambda_archive_request_ingest.role_name}"
  policy = "${module.archivist_topic.publish_policy}"
}

resource "aws_iam_role_policy" "lambda_archive_request_ingest_archive_progress_table" {
  role   = "${module.lambda_archive_request_ingest.role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# Bagger

resource "aws_iam_role_policy" "bagger_task_get_s3" {
  role   = "${module.bagger.task_role_name}"
  policy = "${data.aws_iam_policy_document.bagger_get.json}"
}

resource "aws_iam_role_policy" "bagger_task_put_s3" {
  role   = "${module.bagger.task_role_name}"
  policy = "${data.aws_iam_policy_document.bagger_store.json}"
}

resource "aws_iam_role_policy" "bagger_task_sqs" {
  role   = "${module.bagger.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_bagger_queue.json}"
}

resource "aws_iam_role_policy" "bagger_task_get_s3_dlcs" {
  role   = "${module.bagger.task_role_name}"
  policy = "${data.aws_iam_policy_document.bagger_get_dlcs.json}"
}

resource "aws_iam_role_policy" "bagger_task_get_s3_preservica" {
  role   = "${module.bagger.task_role_name}"
  policy = "${data.aws_iam_policy_document.bagger_get_preservica.json}"
}
