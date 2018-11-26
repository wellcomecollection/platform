# Archivist

resource "aws_iam_role_policy" "archivist_task_store_s3" {
  role   = "${module.archivist.task_role_name}"
  policy = "${var.archive_store_policy_json}"
}

resource "aws_iam_role_policy" "archivist_task_get_s3" {
  role   = "${module.archivist.task_role_name}"
  policy = "${var.ingest_get_policy_json}"
}

resource "aws_iam_role_policy" "archivist_task_get_s3_bagger" {
  role   = "${module.archivist.task_role_name}"
  policy = "${var.ingest_get_policy_json}"
}

resource "aws_iam_role_policy" "archivist_task_get_s3_workflow" {
  role   = "${module.archivist.task_role_name}"
  policy = "${var.ingest_get_policy_json}"
}

resource "aws_iam_role_policy" "archivist_task_registrar_sns" {
  role   = "${module.archivist.task_role_name}"
  policy = "${module.registrar_topic.publish_policy}"
}

resource "aws_iam_role_policy" "archivist_task_progress_async_sns" {
  role   = "${module.archivist.task_role_name}"
  policy = "${module.progress_async_topic.publish_policy}"
}

resource "aws_iam_role_policy" "archivist_task_sqs" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_archivist_queue.json}"
}

# Registrar async

resource "aws_iam_role_policy" "registrar_async_task_get_s3" {
  role   = "${module.bags_async.task_role_name}"
  policy = "${var.archive_get_policy_json}"
}

resource "aws_iam_role_policy" "registrar_async_task_vhs" {
  role   = "${module.bags_async.task_role_name}"
  policy = "${var.vhs_archive_manifest_full_access_policy_json}"
}

resource "aws_iam_role_policy" "registrar_async_task_progress_async_sns" {
  role   = "${module.bags_async.task_role_name}"
  policy = "${module.progress_async_topic.publish_policy}"
}

resource "aws_iam_role_policy" "registrar_async_task_sqs" {
  role   = "${module.bags_async.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_registrar_queue.json}"
}

# Bags (AKA Registrar) http

resource "aws_iam_role_policy" "bags_vhs" {
  role   = "${module.api.bags_role_name}"
  policy = "${var.vhs_archive_manifest_read_policy_json}"
}

# Progress Async

resource "aws_iam_role_policy" "progress_async_task_sns" {
  role   = "${module.ingests_async.task_role_name}"
  policy = "${module.notifier_topic.publish_policy}"
}

resource "aws_iam_role_policy" "progress_async_task_sqs" {
  role   = "${module.ingests_async.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_progress_async_queue.json}"
}

resource "aws_iam_role_policy" "progress_async_task_archive_progress_table" {
  role   = "${module.ingests_async.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# Ingests (AKA Progress) Http

resource "aws_iam_role_policy" "ingests_archive_progress_table" {
  role   = "${module.api.ingests_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

resource "aws_iam_role_policy" "ingests_sns" {
  role   = "${module.api.ingests_role_name}"
  policy = "${module.ingest_requests_topic.publish_policy}"
}

# Notifier

resource "aws_iam_role_policy" "notifier_task_sqs" {
  role   = "${module.notifier.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_notifier_queue.json}"
}

resource "aws_iam_role_policy" "notifier_task_publish_progress_sns" {
  role   = "${module.notifier.task_role_name}"
  policy = "${module.progress_async_topic.publish_policy}"
}

# Bagger

resource "aws_iam_role_policy" "bagger_task_get_s3" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_get_policy_json}"
}

resource "aws_iam_role_policy" "bagger_task_put_s3" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_store_policy_json}"
}

resource "aws_iam_role_policy" "bagger_task_sqs" {
  role   = "${module.bagger.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_bagger_queue.json}"
}

resource "aws_iam_role_policy" "bagger_task_get_s3_dlcs" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_get_dlcs_policy_json}"
}

resource "aws_iam_role_policy" "bagger_task_get_s3_preservica" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_get_preservica_policy_json}"
}

resource "aws_iam_role_policy" "bagger_task_bagging_complete_sns" {
  role   = "${module.bagger.task_role_name}"
  policy = "${module.bagging_complete_topic.publish_policy}"
}
