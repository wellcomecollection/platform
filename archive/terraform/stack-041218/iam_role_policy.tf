# bags aka registrar-async

resource "aws_iam_role_policy" "bags_archive_get" {
  role   = "${module.bags.task_role_name}"
  policy = "${var.archive_get_policy_json}"
}

resource "aws_iam_role_policy" "bags_vhs_write" {
  role   = "${module.bags.task_role_name}"
  policy = "${var.vhs_archive_manifest_full_access_policy_json}"
}

# archivist-nvm

resource "aws_iam_role_policy" "archivist-nvm_task_store_s3" {
  role   = "${module.archivist-nvm.task_role_name}"
  policy = "${var.archive_store_policy_json}"
}

resource "aws_iam_role_policy" "archivist-nvm_task_get_s3" {
  role   = "${module.archivist-nvm.task_role_name}"
  policy = "${var.ingest_get_policy_json}"
}

resource "aws_iam_role_policy" "archivist-nvm_task_get_s3_bagger" {
  role   = "${module.archivist-nvm.task_role_name}"
  policy = "${var.ingest_get_policy_json}"
}

resource "aws_iam_role_policy" "archivist-nvm_task_get_s3_workflow" {
  role   = "${module.archivist-nvm.task_role_name}"
  policy = "${var.ingest_get_policy_json}"
}

# api.bags aka registrar-http

resource "aws_iam_role_policy" "bags_vhs" {
  role   = "${module.api.bags_role_name}"
  policy = "${var.vhs_archive_manifest_read_policy_json}"
}

# api.ingests aka progress-http

resource "aws_iam_role_policy" "api-ingests_archive_progress_table" {
  role   = "${module.api.ingests_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# ingests aka progress-async

resource "aws_iam_role_policy" "ingests_archive_progress_table" {
  role   = "${module.ingests.task_role_name}"
  policy = "${data.aws_iam_policy_document.archive_progress_table_read_write_policy.json}"
}

# bagger-nvm

resource "aws_iam_role_policy" "bagger-nvm_task_queue_discovery" {
  role   = "${module.bagger-nvm.task_role_name}"
  policy = "${data.aws_iam_policy_document.bagger_queue_discovery.json}"
}

resource "aws_iam_role_policy" "bagger-nvm_task_get_s3" {
  role   = "${module.bagger-nvm.task_role_name}"
  policy = "${var.bagger_get_policy_json}"
}

resource "aws_iam_role_policy" "bagger-nvm_task_put_s3" {
  role   = "${module.bagger-nvm.task_role_name}"
  policy = "${var.bagger_store_policy_json}"
}

resource "aws_iam_role_policy" "bagger-nvm_task_get_s3_dlcs" {
  role   = "${module.bagger-nvm.task_role_name}"
  policy = "${var.bagger_get_dlcs_policy_json}"
}

resource "aws_iam_role_policy" "bagger-nvm_task_get_s3_preservica" {
  role   = "${module.bagger-nvm.task_role_name}"
  policy = "${var.bagger_get_preservica_policy_json}"
}
