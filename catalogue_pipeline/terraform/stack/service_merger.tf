# Input queue

module "merger_queue" {
  source = "../modules/queue"

  # Reads from merger
  topic_names = ["${module.matcher_topic.name}"]
  role_names  = ["${module.merger.task_role_name}"]

  namespace = "${var.namespace}_merger"

  visibility_timeout_seconds = 30
  max_receive_count          = 4

  aws_region    = "${var.aws_region}"
  account_id    = "${var.account_id}"
  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}

# Service

module "merger" {
  source = "../modules/service"

  security_group_ids = [
    "${module.egress_security_group.sg_id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  service_name = "${var.namespace}_merger"
  aws_region   = "${var.aws_region}"

  env_vars = {
    metrics_namespace        = "${var.namespace}_merger"
    messages_bucket_name     = "${aws_s3_bucket.messages.id}"
    topic_arn                = "${module.matcher_topic.arn}"
    merger_queue_id          = "${module.merger_queue.url}"
    merger_topic_arn         = "${module.merger_topic.arn}"
    vhs_recorder_bucket_name = "${module.vhs_recorder.bucket_name}"
    vhs_recorder_table_name  = "${module.vhs_recorder.table_name}"
  }

  env_vars_length = 7

  secret_env_vars = {}

  secret_env_vars_length = "0"

  container_image = "${local.merger_image}"
}

# Permissions

resource "aws_iam_role_policy" "merger_vhs_recorder_read" {
  role   = "${module.merger.task_role_name}"
  policy = "${module.vhs_recorder.read_policy}"
}

# Output topic

module "merger_topic" {
  source = "../modules/topic"

  name       = "${var.namespace}_merger"
  role_names = ["${module.merger.task_role_name}"]

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}
