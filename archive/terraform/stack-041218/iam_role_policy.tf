# archivist

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

# api.bags aka registrar-http

resource "aws_iam_role_policy" "bags_vhs" {
  role   = "${module.api.bags_role_name}"
  policy = "${var.vhs_archive_manifest_read_policy_json}"
}

# ingests aka progress-http

resource "aws_iam_role_policy" "ingests_archive_progress_table" {
  role   = "${module.api.ingests_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# bagger

resource "aws_iam_role_policy" "bagger_task_queue_discovery" {
  role   = "${module.bagger.task_role_name}"
  policy = "${data.aws_iam_policy_document.bagger_queue_discovery.json}"
}

resource "aws_iam_role_policy" "bagger_task_get_s3" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_get_policy_json}"
}

resource "aws_iam_role_policy" "bagger_task_put_s3" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_store_policy_json}"
}

resource "aws_iam_role_policy" "bagger_task_get_s3_dlcs" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_get_dlcs_policy_json}"
}

resource "aws_iam_role_policy" "bagger_task_get_s3_preservica" {
  role   = "${module.bagger.task_role_name}"
  policy = "${var.bagger_get_preservica_policy_json}"
}
