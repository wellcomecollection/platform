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
  policy = "${module.vhs_archive_manifest.read_policy}"
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