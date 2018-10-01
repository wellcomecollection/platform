module "catalogue_pipelines" {
  source = "pipelines"

  namespace = "catalogue_pipeline"

  miro_adapter_topic_names = [
    "${local.miro_reindexer_topic_name}",
  ]

  miro_adapter_topic_count = 1

  sierra_adapter_topic_names = [
    "${local.sierra_reindexer_topic_name}",
    "${local.sierra_merged_bibs_topic_name}",
    "${local.sierra_merged_items_topic_name}",
  ]

  sierra_adapter_topic_count = 3

  index_v1 = "v1-2018-09-27-marc-610-subjects"
  index_v2 = "v2-2018-09-27-marc-610-subjects"

  transformer_miro_container_image   = "${local.transformer_miro_container_image}"
  transformer_sierra_container_image = "${local.transformer_sierra_container_image}"
  recorder_container_image           = "${local.recorder_container_image}"
  matcher_container_image            = "${local.matcher_container_image}"
  merger_container_image             = "${local.merger_container_image}"
  id_minter_container_image          = "${local.id_minter_container_image}"
  ingestor_container_image           = "${local.ingestor_container_image}"

  private_subnets = ["${local.private_subnets}"]
  vpc_id          = "${local.vpc_id}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  vhs_miro_read_policy = "${local.vhs_miro_read_policy}"

  vhs_sierra_read_policy = "${local.vhs_sierra_read_policy}"

  aws_region      = "${var.aws_region}"
  messages_bucket = "${aws_s3_bucket.messages.id}"
  infra_bucket    = "${var.infra_bucket}"

  rds_access_security_group_id = "${local.rds_access_security_group_id}"

  identifiers_rds_cluster_password = "${local.identifiers_rds_cluster_password}"
  identifiers_rds_cluster_username = "${local.identifiers_rds_cluster_username}"
  identifiers_rds_cluster_port     = "${local.identifiers_rds_cluster_port}"
  identifiers_rds_cluster_host     = "${local.identifiers_rds_cluster_host}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  dlq_alarm_arn          = "${local.dlq_alarm_arn}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  service_egress_security_group_id = "${module.service_egress_security_group.sg_id}"

  vhs_bucket_name = "${aws_s3_bucket.vhs_bucket.id}"
}
