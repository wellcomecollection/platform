module "v1_pipeline" {
  source = "v1_pipeline"

  namespace = "${var.namespace}_v1"

  transformed_works_topic_name = "${module.transformed_miro_works_topic.name}"
  index                        = "${var.index_v1}"

  id_minter_container_image = "${var.id_minter_container_image}"
  ingestor_container_image  = "${var.ingestor_container_image}"

  subnets         = ["${var.private_subnets}"]
  vpc_id          = "${var.vpc_id}"
  account_id      = "${var.account_id}"
  aws_region      = "${var.aws_region}"
  messages_bucket = "${var.messages_bucket}"

  rds_access_security_group_id     = "${var.rds_access_security_group_id}"
  identifiers_rds_cluster_password = "${var.identifiers_rds_cluster_password}"
  identifiers_rds_cluster_username = "${var.identifiers_rds_cluster_username}"
  identifiers_rds_cluster_port     = "${var.identifiers_rds_cluster_port}"
  identifiers_rds_cluster_host     = "${var.identifiers_rds_cluster_host}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  dlq_alarm_arn          = "${var.dlq_alarm_arn}"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  cluster_name                       = "${aws_ecs_cluster.cluster.name}"
  cluster_id                         = "${aws_ecs_cluster.cluster.id}"
  namespace_id                       = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  allow_s3_messages_put_json         = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
  allow_s3_messages_get_json         = "${data.aws_iam_policy_document.allow_s3_messages_get.json}"
  allow_cloudwatch_push_metrics_json = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

module "v2_pipeline" {
  source = "v2_pipeline"

  namespace = "${var.namespace}_v2"

  transformed_works_topic_names = ["${module.transformed_miro_works_topic.name}", "${module.transformed_sierra_works_topic.name}"]
  transformed_works_topic_count = 2
  index                         = "${var.index_v2}"

  recorder_container_image  = "${var.recorder_container_image}"
  matcher_container_image   = "${var.matcher_container_image}"
  merger_container_image    = "${var.merger_container_image}"
  id_minter_container_image = "${var.id_minter_container_image}"
  ingestor_container_image  = "${var.ingestor_container_image}"

  vhs_bucket_name = "${var.vhs_bucket_name}"

  subnets         = ["${var.private_subnets}"]
  vpc_id          = "${var.vpc_id}"
  account_id      = "${var.account_id}"
  aws_region      = "${var.aws_region}"
  messages_bucket = "${var.messages_bucket}"
  infra_bucket    = "${var.infra_bucket}"

  rds_access_security_group_id     = "${var.rds_access_security_group_id}"
  identifiers_rds_cluster_password = "${var.identifiers_rds_cluster_password}"
  identifiers_rds_cluster_username = "${var.identifiers_rds_cluster_username}"
  identifiers_rds_cluster_port     = "${var.identifiers_rds_cluster_port}"
  identifiers_rds_cluster_host     = "${var.identifiers_rds_cluster_host}"

  es_cluster_credentials             = "${var.es_cluster_credentials}"
  dlq_alarm_arn                      = "${var.dlq_alarm_arn}"
  lambda_error_alarm_arn             = "${var.lambda_error_alarm_arn}"
  service_egress_security_group_id   = "${var.service_egress_security_group_id}"
  cluster_name                       = "${aws_ecs_cluster.cluster.name}"
  cluster_id                         = "${aws_ecs_cluster.cluster.id}"
  namespace_id                       = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  allow_s3_messages_put_json         = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
  allow_s3_messages_get_json         = "${data.aws_iam_policy_document.allow_s3_messages_get.json}"
  allow_cloudwatch_push_metrics_json = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
