# Input queue

module "ingestor_queue" {
  source = "../modules/queue"

  topic_names = ["${module.id_minter_topic.name}"]
  role_names  = ["${module.ingestor.task_role_name}"]

  namespace = "${var.namespace}_ingestor"

  visibility_timeout_seconds = 30
  max_receive_count          = 6

  aws_region    = "${var.aws_region}"
  account_id    = "${var.account_id}"
  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  messages_bucket_arn = "${aws_s3_bucket.messages.arn}"
}

# Service

module "ingestor" {
  source = "../modules/service"

  service_name = "${var.namespace}_ingestor"

  container_image = "${local.ingestor_image}"

  security_group_ids = [
    "${module.egress_security_group.sg_id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  aws_region   = "${var.aws_region}"

  env_vars = {
    metrics_namespace = "${var.namespace}_ingestor"
    es_index          = "${var.es_works_index}"
    ingest_queue_id   = "${module.ingestor_queue.url}"
  }

  env_vars_length = 4

  secret_env_vars = {
    es_host     = "catalogue/ingestor/es_host"
    es_port     = "catalogue/ingestor/es_port"
    es_username = "catalogue/ingestor/es_username"
    es_password = "catalogue/ingestor/es_password"
    es_protocol = "catalogue/ingestor/es_protocol"
  }

  secret_env_vars_length = "5"
}
