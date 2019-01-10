# Stack

resource "aws_api_gateway_base_path_mapping" "api-gw-mapping-041218" {
  api_id      = "${module.stack-041218.api_gateway_id}"
  domain_name = "storage.api.wellcomecollection.org"
  base_path   = "storage"
}

module "stack-041218" {
  source = "stack-041218"

  namespace = "storage-041218"

  domain_name = "api.wellcomecollection.org"

  vpc_id   = "${local.vpc_id}"
  vpc_cidr = "${local.vpc_cidr}"

  private_subnets = "${local.private_subnets}"
  public_subnets  = "${local.public_subnets}"

  ssh_key_name = "${var.key_name}"

  controlled_access_cidr_ingress = ["${var.admin_cidr_ingress}"]

  current_account_id = "${data.aws_caller_identity.current.account_id}"
  dlq_alarm_arn      = "${local.dlq_alarm_arn}"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"
  interservice_security_group_id   = "${aws_security_group.interservice.id}"

  cognito_user_pool_arn          = "${local.cognito_user_pool_arn}"
  cognito_storage_api_identifier = "${local.cognito_storage_api_identifier}"

  registrar_async_container_image      = "${local.registrar_async_container_image}"
  progress_async_container_image       = "${local.progress_async_container_image}"
  progress_http_container_image        = "${local.progress_http_container_image}"
  registrar_http_container_image       = "${local.registrar_http_container_image}"
  archivist_container_image            = "${local.archivist_container_image}"
  notifier_container_image             = "${local.notifier_container_image}"
  nginx_container_image                = "${local.nginx_container_image}"
  bagger_container_image               = "${local.bagger_container_image}"

  archive_bucket_name                = "${aws_s3_bucket.archive_storage.bucket}"
  storage_static_content_bucket_name = "${aws_s3_bucket.storage_static_content.bucket}"
  vhs_archive_manifest_table_name    = "${module.vhs_archive_manifest.table_name}"
  vhs_archive_manifest_bucket_name   = "${module.vhs_archive_manifest.bucket_name}"

  bagger_dlcs_space                  = "${var.bagger_dlcs_space}"
  bagger_dds_api_key                 = "${var.bagger_dds_api_key}"
  bagger_working_directory           = "${var.bagger_working_directory}"
  bagger_dlcs_entry                  = "${var.bagger_dlcs_entry}"
  bagger_dlcs_api_key                = "${var.bagger_dlcs_api_key}"
  bagger_dlcs_api_secret             = "${var.bagger_dlcs_api_secret}"
  bagger_mets_bucket_name            = "${var.bagger_mets_bucket_name}"
  bagger_dlcs_customer_id            = "${var.bagger_dlcs_customer_id}"
  bagger_drop_bucket_name            = "${var.bagger_drop_bucket_name}"
  bagger_drop_bucket_name_errors     = "${var.bagger_drop_bucket_name_errors}"
  bagger_read_mets_from_fileshare    = "${var.bagger_read_mets_from_fileshare}"
  bagger_dlcs_source_bucket          = "${var.bagger_dlcs_source_bucket}"
  bagger_drop_bucket_name_mets_only  = "${var.bagger_drop_bucket_name_mets_only}"
  bagger_current_preservation_bucket = "${var.bagger_current_preservation_bucket}"
  bagger_dds_api_secret              = "${var.bagger_dds_api_secret}"
  bagger_dds_asset_prefix            = "${var.bagger_dds_asset_prefix}"

  archive_store_policy_json                    = "${data.aws_iam_policy_document.archive_store.json}"
  archive_get_policy_json                      = "${data.aws_iam_policy_document.archive_get.json}"
  bagger_get_preservica_policy_json            = "${data.aws_iam_policy_document.bagger_get_preservica.json}"
  bagger_get_dlcs_policy_json                  = "${data.aws_iam_policy_document.bagger_get_dlcs.json}"
  bagger_get_policy_json                       = "${data.aws_iam_policy_document.bagger_get.json}"
  bagger_store_policy_json                     = "${data.aws_iam_policy_document.bagger_store.json}"
  ingest_get_policy_json                       = "${data.aws_iam_policy_document.ingest_get.json}"
  vhs_archive_manifest_full_access_policy_json = "${module.vhs_archive_manifest.full_access_policy}"
  vhs_archive_manifest_read_policy_json        = "${module.vhs_archive_manifest.read_policy}"

  alarm_topic_arn = "${local.gateway_server_error_alarm_arn}"
}
