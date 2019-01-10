module "miro_transformer" {
  source = "transformer"

  source_name = "miro"

  adapter_topic_names                    = "${var.miro_adapter_topic_names}"
  adapter_topic_count                    = "${var.miro_adapter_topic_count}"
  transformed_works_topic_publish_policy = "${module.transformed_miro_works_topic.publish_policy}"
  transformed_works_topic_arn            = "${module.transformed_miro_works_topic.arn}"

  vhs_read_policy = "${var.vhs_miro_read_policy}"

  messages_bucket = "${var.messages_bucket}"

  transformer_container_image      = "${var.transformer_miro_container_image}"
  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  subnets                          = "${var.private_subnets}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace    = "${var.namespace}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  allow_s3_messages_put_json         = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
  allow_cloudwatch_push_metrics_json = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"

  aws_region = "${var.aws_region}"
  account_id = "${var.account_id}"

  vpc_id = "${var.vpc_id}"
}

module "sierra_transformer" {
  source = "transformer"

  source_name = "sierra"

  adapter_topic_names                    = "${var.sierra_adapter_topic_names}"
  adapter_topic_count                    = "${var.sierra_adapter_topic_count}"
  transformed_works_topic_publish_policy = "${module.transformed_sierra_works_topic.publish_policy}"
  transformed_works_topic_arn            = "${module.transformed_sierra_works_topic.arn}"

  vhs_read_policy = "${var.vhs_sierra_read_policy}"

  messages_bucket = "${var.messages_bucket}"

  transformer_container_image      = "${var.transformer_sierra_container_image}"
  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  subnets                          = "${var.private_subnets}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace    = "${var.namespace}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  allow_s3_messages_put_json         = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
  allow_cloudwatch_push_metrics_json = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"

  aws_region = "${var.aws_region}"
  account_id = "${var.account_id}"

  vpc_id = "${var.vpc_id}"
}
