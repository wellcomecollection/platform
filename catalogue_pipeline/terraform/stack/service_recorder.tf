# Input queue

module "recorder_queue" {
  source = "../modules/queue"

  topic_names = [
    "${module.miro_transformer_topic.name}",
    "${module.sierra_transformer_topic.name}",
  ]

  role_names = ["${module.recorder.task_role_name}"]

  namespace = "${var.namespace}_recorder"

  visibility_timeout_seconds = 60
  max_receive_count          = 8

  aws_region    = "${var.aws_region}"
  account_id    = "${var.account_id}"
  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}

# Service

module "recorder" {
  source = "../modules/service"

  security_group_ids = [
    "${module.egress_security_group.sg_id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  service_name = "${var.namespace}_recorder"
  aws_region   = "${var.aws_region}"

  env_vars = {
    recorder_queue_url = "${module.recorder_queue.url}"
    metrics_namespace  = "${var.namespace}_recorder"

    vhs_recorder_dynamo_table_name = "${module.vhs_recorder.table_name}"
    vhs_recorder_bucket_name       = "${module.vhs_recorder.bucket_name}"

    sns_topic = "${module.recorder_topic.arn}"
  }

  env_vars_length = 6

  secret_env_vars = {}

  secret_env_vars_length = "0"

  container_image = "${local.recorder_image}"
}

# Permissions

resource "aws_iam_role_policy" "recorder_vhs_recorder_readwrite" {
  role   = "${module.recorder.task_role_name}"
  policy = "${module.vhs_recorder.full_access_policy}"
}

# Output topic

module "recorder_topic" {
  source = "../modules/topic"

  name       = "${var.namespace}_recorder"
  role_names = ["${module.recorder.task_role_name}"]

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}
