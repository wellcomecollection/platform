module "critical" {
  source = "critical"

  vpc_id  = "${local.vpc_id}"
  subnets = ["${local.private_subnets}"]

  namespace = "catalogue"
}

module "catalogue_pipeline_2019_01_24" {
  source = "stack"

  namespace = "catalogue-2019-01-24"

  release_label = "latest"

  account_id = "${data.aws_caller_identity.current.account_id}"
  aws_region = "${local.aws_region}"
  vpc_id = "${local.vpc_id}"
  subnets = ["${local.private_subnets}"]

  dlq_alarm_arn = "${local.dlq_alarm_arn}"

  # Elasticsearch

  es_works_credentials = {

  }

  es_works_index = ""

  # RDS

  rds_ids_credentials = {

  }

  rds_ids_access_security_group_id = "${local.rds_access_security_group_id}"

  # Messaging

  messages_bucket_id  = "${module.critical.messages_bucket_id}"
  messages_bucket_arn = "${module.critical.messages_bucket_arn}"

  # VHS

  vhs_recorder_bucket_id = "${module.critical.recorder_vhs_bucket_id}"

  vhs_miro_read_policy = "${local.vhs_miro_read_policy}"
  vhs_sierra_read_policy = "${local.vhs_sierra_read_policy}"

  # Transformer config

  sierra_adapter_topic_names = [
    "${local.sierra_reindexer_topic_name}",
    "${local.sierra_merged_bibs_topic_name}",
    "${local.sierra_merged_items_topic_name}",
  ]

  sierra_adapter_topic_count = 3

  miro_adapter_topic_names = [
    "${local.miro_reindexer_topic_name}",
    "${local.miro_updates_topic_name}",
  ]

  miro_adapter_topic_count = "2"
}
