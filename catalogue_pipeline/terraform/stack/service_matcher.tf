# Input queue

module "matcher_queue" {
  source = "../modules/queue"

  topic_names = ["${module.recorder_topic.name}"]
  role_names = ["${module.matcher.task_role_name}"]

  namespace = "${var.namespace}_matcher"

  visibility_timeout_seconds = 30
  max_receive_count          = 3

  aws_region = "${var.aws_region}"
  account_id = "${var.account_id}"
  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}

# Service

module "matcher" {
  source = "../modules/service"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  service_name = "${var.namespace}_matcher"
  aws_region   = "${var.aws_region}"

  env_vars = {
    queue_url               = "${module.matcher_queue.url}"
    metrics_namespace       = "${var.namespace}_matcher"
    vhs_bucket_name         = "${module.vhs_recorder.bucket_name}"
    topic_arn               = "${module.matcher_topic.arn}"

    dynamo_table            = "${aws_dynamodb_table.matcher_graph_table.id}"
    dynamo_index            = "work-sets-index"
    dynamo_lock_table       = "${aws_dynamodb_table.matcher_lock_table.id}"
    dynamo_lock_table_index = "context-ids-index"
  }

  env_vars_length = 8

  secret_env_vars = {}

  secret_env_vars_length = "0"

  container_image   = "${local.matcher_image}"
}

# Permissions

resource "aws_iam_role_policy" "matcher_vhs_recorder_read" {
  role   = "${module.matcher.task_role_name}"
  policy = "${module.vhs_recorder.read_policy}"
}

resource "aws_iam_role_policy" "matcher_graph_readwrite" {
  role   = "${module.matcher.task_role_name}"
  policy = "${data.aws_iam_policy_document.graph_table_read_write_policy.json}"
}

resource "aws_iam_role_policy" "matcher_lock_readwrite" {
  role   = "${module.matcher.task_role_name}"
  policy = "${data.aws_iam_policy_document.lock_table_read_write_policy.json}"
}

# Output topic

module "matcher_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${var.namespace}_matcher"
}