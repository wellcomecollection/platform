module "items_window_generator" {
  source = "sierra_window_generator"

  resource_type = "items"

  window_length_minutes    = 31
  trigger_interval_minutes = 15

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}

module "items_reader" {
  source = "sierra_reader"

  resource_type = "items"

  bucket_name        = "${aws_s3_bucket.sierra_adapter.id}"
  windows_topic_name = "${module.items_window_generator.topic_name}"

  sierra_fields = "${var.sierra_items_fields}"

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"

  release_id = "${var.release_ids["sierra_reader"]}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${local.vpc_id}"

  dlq_alarm_arn          = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  infra_bucket = "${var.infra_bucket}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = ["${local.private_subnets}"]

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"
  interservice_security_group_id   = "${aws_security_group.interservice_security_group.id}"

  sierra_reader_ecr_repository_url = "${module.ecr_repository_sierra_reader.repository_url}"
}

module "items_to_dynamo" {
  source = "items_to_dynamo"

  demultiplexer_topic_name = "${module.items_reader.topic_name}"
  release_id               = "${var.release_ids["sierra_items_to_dynamo"]}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${local.vpc_id}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = ["${local.private_subnets}"]

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"
  interservice_security_group_id   = "${aws_security_group.interservice_security_group.id}"
}

module "items_merger" {
  source = "item_merger"

  resource_type = "items"
  release_id    = "${var.release_ids["sierra_item_merger"]}"

  merged_dynamo_table_name = "${local.vhs_table_name}"

  updates_topic_name         = "${module.items_to_dynamo.topic_name}"
  reindexed_items_topic_name = "${local.reindexed_items_topic_name}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${local.vpc_id}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  account_id = "${data.aws_caller_identity.current.account_id}"

  vhs_full_access_policy = "${local.vhs_full_access_policy}"

  bucket_name = "${local.vhs_bucket_name}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = ["${local.private_subnets}"]

  sierra_items_bucket = "${module.items_to_dynamo.vhs_bucket_name}"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"
  interservice_security_group_id   = "${aws_security_group.interservice_security_group.id}"
}
