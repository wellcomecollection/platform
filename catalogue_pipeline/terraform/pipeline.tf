module "catalogue_pipeline" {
  source = "pipeline"

  namespace = "catalogue_pipeline"

  transformer_miro_container_image   = "${local.transformer_miro_container_image}"
  transformer_sierra_container_image = "${local.transformer_sierra_container_image}"
  recorder_container_image           = "${local.recorder_container_image}"
  matcher_container_image            = "${local.matcher_container_image}"
  merger_container_image             = "${local.merger_container_image}"
  id_minter_container_image          = "${local.id_minter_container_image}"
  ingestor_container_image           = "${local.ingestor_container_image}"

  subnets = ["${local.private_subnets}"]
  vpc_id  = "${local.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  vhs_miro_read_policy      = "${local.vhs_miro_read_policy}"
  vhs_miro_bucket_name      = "${local.vhs_miro_bucket_name}"
  vhs_miro_table_stream_arn = "${local.vhs_miro_table_stream_arn}"

  vhs_sierra_read_policy      = "${local.vhs_sierra_read_policy}"
  vhs_sierra_bucket_name      = "${local.vhs_sierra_bucket_name}"
  vhs_sierra_table_stream_arn = "${local.vhs_sierra_table_stream_arn}"

  vhs_recorder_table_name         = "${module.vhs_recorder.table_name}"
  vhs_recorder_bucket_name        = "${module.vhs_recorder.bucket_name}"
  vhs_recorder_read_policy        = "${module.vhs_recorder.read_policy}"
  vhs_recorder_full_access_policy = "${module.vhs_recorder.full_access_policy}"

  aws_region      = "${var.aws_region}"
  messages_bucket = "${aws_s3_bucket.messages.id}"
  infra_bucket    = "${var.infra_bucket}"

  index_v1 = "v1-2018-09-11-s3-sns-maybe"
  index_v2 = "v2-2018-09-11-s3-sns-maybe"

  rds_access_security_group_id = "${local.rds_access_security_group_id}"

  identifiers_rds_cluster_password = "${local.identifiers_rds_cluster_password}"
  identifiers_rds_cluster_username = "${local.identifiers_rds_cluster_username}"
  identifiers_rds_cluster_port     = "${local.identifiers_rds_cluster_port}"
  identifiers_rds_cluster_host     = "${local.identifiers_rds_cluster_host}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  dlq_alarm_arn          = "${local.dlq_alarm_arn}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  miro_adapter_topic_names = [
    "${local.miro_reindexer_topic_name}",
  ]

  sierra_adapter_topic_names = [
    "${local.sierra_reindexer_topic_name}",
    "${local.sierra_merged_bibs_topic_name}",
    "${local.sierra_merged_items_topic_name}",
  ]
}
