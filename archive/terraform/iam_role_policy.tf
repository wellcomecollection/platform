# Archivist

resource "aws_iam_role_policy" "archivist_task_store_s3" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_store.json}"
}

resource "aws_iam_role_policy" "archivist_task_get_s3" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.ingest_get.json}"
}

resource "aws_iam_role_policy" "archivist_task_get_s3_bagger" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.ingest_get_bagger.json}"
}

resource "aws_iam_role_policy" "archivist_task_get_s3_workflow" {
  role   = "${module.archivist.task_role_name}"
  policy = "${data.aws_iam_policy_document.ingest_workflow_get.json}"
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
  role   = "${module.registrar_async.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_get.json}"
}

resource "aws_iam_role_policy" "registrar_async_task_vhs" {
  role   = "${module.registrar_async.task_role_name}"
  policy = "${module.vhs_archive_manifest.full_access_policy}"
}

resource "aws_iam_role_policy" "registrar_async_task_progress_async_sns" {
  role   = "${module.registrar_async.task_role_name}"
  policy = "${module.progress_async_topic.publish_policy}"
}

resource "aws_iam_role_policy" "registrar_async_task_sqs" {
  role   = "${module.registrar_async.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_registrar_queue.json}"
}

# Bags (AKA Registrar) http

resource "aws_iam_role_policy" "bags_vhs" {
  role   = "${module.storage_api.bags_role_name}"
  policy = "${module.vhs_archive_manifest.read_policy}"
}

# Progress Async

resource "aws_iam_role_policy" "progress_async_task_sns" {
  role   = "${module.progress_async.task_role_name}"
  policy = "${module.notifier_topic.publish_policy}"
}

resource "aws_iam_role_policy" "progress_async_task_sqs" {
  role   = "${module.progress_async.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_progress_async_queue.json}"
}

resource "aws_iam_role_policy" "progress_async_task_archive_progress_table" {
  role   = "${module.progress_async.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# Ingests (AKA Progress) Http

resource "aws_iam_role_policy" "ingests_archive_progress_table" {
  role   = "${module.storage_api.ingests_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

resource "aws_iam_role_policy" "ingests_sns" {
  role   = "${module.storage_api.ingests_role_name}"
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

resource "aws_iam_role_policy" "bagger_task_bagging_complete_sns" {
  role   = "${module.bagger.task_role_name}"
  policy = "${module.bagging_complete_topic.publish_policy}"
}
