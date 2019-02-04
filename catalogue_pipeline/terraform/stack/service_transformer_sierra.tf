# Input queue

module "sierra_transformer_queue" {
  source = "../modules/queue"

  topic_names = ["${var.sierra_adapter_topic_names}"]

  role_names = ["${module.sierra_transformer.task_role_name}"]

  namespace = "${var.namespace}_sierra_transformer"

  visibility_timeout_seconds = 30
  max_receive_count          = 3

  aws_region    = "${var.aws_region}"
  account_id    = "${var.account_id}"
  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}

# Service

module "sierra_transformer" {
  source = "../modules/service"

  service_name = "${var.namespace}_sierra_transformer"

  container_image = "${local.transformer_sierra_image}"

  security_group_ids = [
    "${module.egress_security_group.sg_id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  env_vars = {
    sns_arn              = "${module.sierra_transformer_topic.arn}"
    transformer_queue_id = "${module.sierra_transformer_queue.url}"
    metrics_namespace    = "sierra_transformer"
    messages_bucket_name = "${aws_s3_bucket.messages.id}"
  }

  env_vars_length = 4

  secret_env_vars        = {}
  secret_env_vars_length = "0"

  subnets    = ["${var.subnets}"]
  aws_region = "${var.aws_region}"
}

# Permissions

resource "aws_iam_role_policy" "sierra_transformer_vhs_sierra_adapter_read" {
  role   = "${module.sierra_transformer.task_role_name}"
  policy = "${var.vhs_sierra_read_policy}"
}

# Output topic

module "sierra_transformer_topic" {
  source = "../modules/topic"

  name       = "${var.namespace}_sierra_transformer"
  role_names = ["${module.sierra_transformer.task_role_name}"]

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}
