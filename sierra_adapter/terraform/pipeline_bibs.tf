module "bibs_window_generator" {
  source = "sierra_window_generator"

  resource_type = "bibs"

  window_length_minutes    = 16
  trigger_interval_minutes = 7

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}

module "bibs_reader" {
  source = "sierra_reader"

  resource_type = "bibs"

  bucket_name        = "wellcomecollection-platform-adapters-sierra" //${aws_s3_bucket.sierra_adapter.id}
  windows_topic_name = "${module.bibs_window_generator.topic_name}"

  sierra_fields = "${var.sierra_bibs_fields}"

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

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  interservice_security_group_id   = "${aws_security_group.interservice_security_group.id}"

  sierra_reader_ecr_repository_url = "${module.ecr_repository_sierra_reader.repository_url}"
}

module "bib_updates_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v6.4.0"
  queue_name  = "sierra_bibs_merger_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.bibs_reader.topic_name}"]

  # Ensure that messages are spread around -- if the merger has an error
  # (for example, hitting DynamoDB write limits), we don't retry too quickly.
  visibility_timeout_seconds = 300

  max_receive_count = 4

  alarm_topic_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}

module "bibs_merger" {
  source = "merger"

  resource_type = "bibs"

  release_id = "${var.release_ids["sierra_bib_merger"]}"

  env_vars = {
    windows_queue_url = "${module.bib_updates_queue.id}"
    metrics_namespace = "sierra_bib_merger"
    dynamo_table_name = "${local.vhs_table_name}"
    bucket_name       = "${local.vhs_bucket_name}"
  }

  env_vars_length = 4

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${local.vpc_id}"

  vhs_full_access_policy = "${local.vhs_full_access_policy}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = ["${local.private_subnets}"]

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  interservice_security_group_id   = "${aws_security_group.interservice_security_group.id}"
}
