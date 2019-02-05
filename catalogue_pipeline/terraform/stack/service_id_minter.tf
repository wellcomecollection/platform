# Input queue

module "id_minter_queue" {
  source = "../modules/queue"

  topic_names = ["${module.merger_topic.name}"]

  role_names = ["${module.id_minter.task_role_name}"]

  namespace = "${var.namespace}_id_minter"

  visibility_timeout_seconds = 60
  max_receive_count          = 5

  aws_region    = "${var.aws_region}"
  account_id    = "${var.account_id}"
  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}

# Service

module "id_minter" {
  source = "../modules/service"

  service_name = "${var.namespace}_id_minter"

  container_image = "${local.id_minter_image}"

  security_group_ids = [
    "${module.egress_security_group.sg_id}",
    "${var.rds_ids_access_security_group_id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  aws_region   = "${var.aws_region}"

  env_vars = {
    metrics_namespace    = "${var.namespace}_id_minter"
    messages_bucket_name = "${aws_s3_bucket.messages.id}"

    queue_url       = "${module.id_minter_queue.url}"
    topic_arn       = "${module.id_minter_topic.arn}"
    max_connections = 8
  }

  env_vars_length = 5

  secret_env_vars = {
    cluster_url = "catalogue/id_minter/rds_host"
    db_port     = "catalogue/id_minter/rds_port"
    db_username = "catalogue/id_minter/rds_user"
    db_password = "catalogue/id_minter/rds_password"
  }

  secret_env_vars_length = "4"
}

# Output topic

module "id_minter_topic" {
  source = "../modules/topic"

  name       = "${var.namespace}_id_minter"
  role_names = ["${module.id_minter.task_role_name}"]

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}
