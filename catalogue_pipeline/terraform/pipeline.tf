module "catalogue_pipeline" {
  source = "pipeline"

  namespace                   = "catalogue_pipeline"
  transformer_container_image = "${local.transformer_container_image}"
  recorder_container_image    = "${local.recorder_container_image}"
  matcher_container_image     = "${local.matcher_container_image}"
  merger_container_image      = "${local.merger_container_image}"
  id_minter_container_image   = "${local.id_minter_container_image}"
  ingestor_container_image    = "${local.ingestor_container_image}"

  subnets                    = ["${local.private_subnets}"]
  vpc_id                     = "${local.vpc_id}"
  account_id                 = "${data.aws_caller_identity.current.account_id}"
  vhs_sourcedata_read_policy = "${module.vhs_sourcedata.read_policy}"
  vhs_sourcedata_bucket_name = "${module.vhs_sourcedata.bucket_name}"
  aws_region                 = "${var.aws_region}"
  messages_bucket            = "${aws_s3_bucket.messages.id}"
  infra_bucket               = "${var.infra_bucket}"

  index_v1 = "v1-2018-07-25"
  index_v2 = "v2-2018-07-25"

  identifiers_rds_cluster_password = "${module.identifiers_delta_rds_cluster.password}"
  identifiers_rds_cluster_username = "${module.identifiers_delta_rds_cluster.username}"
  identifiers_rds_cluster_port     = "${module.identifiers_delta_rds_cluster.port}"
  identifiers_rds_cluster_host     = "${module.identifiers_delta_rds_cluster.host}"

  es_cluster_credentials          = "${var.es_cluster_credentials}"
  dlq_alarm_arn                   = "${local.dlq_alarm_arn}"
  lambda_error_alarm_arn          = "${local.lambda_error_alarm_arn}"
  vhs_sourcedata_table_stream_arn = "${module.vhs_sourcedata.table_stream_arn}"
}
