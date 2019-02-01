module "recorder" {
  source = "../modules/service"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}_recorder"
  aws_region   = "${var.aws_region}"

  env_vars = {
    recorder_queue_url  = "${module.recorder_queue.id}"
    metrics_namespace   = "${var.namespace}_recorder"
    message_bucket_name = "${var.messages_bucket_id}"

    vhs_recorder_dynamo_table_name = "${module.vhs_recorder.table_name}"
    vhs_recorder_bucket_name       = "${module.vhs_recorder.bucket_name}"

    sns_topic = "${module.recorded_works_topic.arn}"
  }

  env_vars_length = 6

  secret_env_vars = {}

  secret_env_vars_length = "0"

  container_image   = "${local.recorder_image}"
  source_queue_name = "${module.recorder_queue.name}"
  source_queue_arn  = "${module.recorder_queue.arn}"
}

module "matcher" {
  source = "../modules/service"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}_matcher"
  aws_region   = "${var.aws_region}"

  env_vars = {
    queue_url               = "${module.matcher_queue.id}"
    metrics_namespace       = "${var.namespace}_matcher"
    vhs_bucket_name         = "${module.vhs_recorder.bucket_name}"
    topic_arn               = "${module.matched_works_topic.arn}"
    dynamo_table            = "${aws_dynamodb_table.matcher_graph_table.id}"
    dynamo_index            = "work-sets-index"
    dynamo_lock_table       = "${aws_dynamodb_table.matcher_lock_table.id}"
    dynamo_lock_table_index = "context-ids-index"
  }

  env_vars_length = 8

  secret_env_vars = {}

  secret_env_vars_length = "0"

  container_image   = "${local.matcher_image}"
  source_queue_name = "${module.matcher_queue.name}"
  source_queue_arn  = "${module.matcher_queue.arn}"
}

module "merger" {
  source = "../modules/service"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}_merger"
  aws_region   = "${var.aws_region}"

  env_vars = {
    metrics_namespace        = "${var.namespace}_merger"
    messages_bucket_name     = "${var.messages_bucket_id}"
    topic_arn                = "${module.matched_works_topic.arn}"
    merger_queue_id          = "${module.merger_queue.id}"
    merger_topic_arn         = "${module.merged_works_topic.arn}"
    vhs_recorder_bucket_name = "${module.vhs_recorder.bucket_name}"
    vhs_recorder_table_name  = "${module.vhs_recorder.table_name}"
  }

  env_vars_length = 7

  secret_env_vars = {}

  secret_env_vars_length = "0"

  container_image   = "${local.merger_image}"
  source_queue_name = "${module.merger_queue.name}"
  source_queue_arn  = "${module.merger_queue.arn}"
}

module "id_minter" {
  source = "../modules/service"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}_id_minter"
  aws_region   = "${var.aws_region}"

  env_vars = {
    metrics_namespace   = "${var.namespace}_id_minter"
    message_bucket_name = "${var.messages_bucket_id}"

    queue_url       = "${module.id_minter_queue.id}"
    topic_arn       = "${module.es_ingest_topic.arn}"
    max_connections = 8
  }

  env_vars_length = 5

  secret_env_vars = {
    cluster_url = "catalogue/id_minter/rds_host"
    db_port     = "catalogue/id_minter/rds_port"
    db_username = "catalogue/id_minter/rds_username"
    db_password = "catalogue/id_minter/rds_password"
  }

  secret_env_vars_length = "4"

  container_image   = "${local.id_minter_image}"
  source_queue_name = "${module.id_minter_queue.name}"
  source_queue_arn  = "${module.id_minter_queue.arn}"

  // Our RDS instance allows a maximum of 45 concurrent connections.
  // The maximum number of concurrent connection is determined by
  // max_connections * max_capacity so we always need to set those
  // two values in a way that their product doesn't exceed 45
  max_capacity = 3

  security_group_ids = ["${var.rds_ids_access_security_group_id}"]
}

module "ingestor" {
  source = "../modules/service"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}_ingestor"
  aws_region   = "${var.aws_region}"

  env_vars = {
    metrics_namespace   = "${var.namespace}_ingestor"
    message_bucket_name = "${var.messages_bucket_id}"
    es_index            = "${var.es_works_index}"
    ingest_queue_id     = "${module.es_ingest_queue.id}"
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

  container_image   = "${local.ingestor_image}"
  source_queue_name = "${module.es_ingest_queue.name}"
  source_queue_arn  = "${module.es_ingest_queue.arn}"

  max_capacity = 5
}

# Transformers

module "miro_transformer" {
  source = "../modules/transformer"

  source_name     = "miro"
  container_image = "${local.transformer_miro_image}"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace    = "${var.namespace}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  adapter_topic_names = ["${var.miro_adapter_topic_names}"]
  adapter_topic_count = "${var.miro_adapter_topic_count}"

  transformed_works_topic_publish_policy = "${module.transformed_miro_works_topic.publish_policy}"
  transformed_works_topic_arn            = "${module.transformed_miro_works_topic.arn}"

  vhs_read_policy = "${var.vhs_sierra_read_policy}"

  message_bucket_name = "${var.messages_bucket_id}"

  subnets = "${var.subnets}"

  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  allow_s3_messages_put_json = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"

  allow_cloudwatch_push_metrics_json = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"

  aws_region = "${var.aws_region}"
  account_id = "${var.account_id}"

  vpc_id = "${var.vpc_id}"
}

module "sierra_transformer" {
  source = "../modules/transformer"

  container_image = "${local.transformer_sierra_image}"

  service_egress_security_group_id = "${module.egress_security_group.sg_id}"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace    = "${var.namespace}_sierra"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  adapter_topic_names = ["${var.sierra_adapter_topic_names}"]
  adapter_topic_count = "${var.sierra_adapter_topic_count}"

  transformed_works_topic_publish_policy = "${module.transformed_sierra_works_topic.publish_policy}"

  transformed_works_topic_arn = "${module.transformed_sierra_works_topic.arn}"

  vhs_read_policy = "${var.vhs_sierra_read_policy}"

  message_bucket_name = "${var.messages_bucket_id}"

  dlq_alarm_arn = "${var.dlq_alarm_arn}"

  allow_s3_messages_put_json         = "${data.aws_iam_policy_document.allow_s3_messages_put.json}"
  allow_cloudwatch_push_metrics_json = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"

  aws_region = "${var.aws_region}"
  account_id = "${var.account_id}"

  vpc_id  = "${var.vpc_id}"
  subnets = "${var.subnets}"
}
