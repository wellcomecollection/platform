module "recorder" {
  source = "../service"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${var.cluster_name}"
  namespace_id                     = "${var.namespace_id}"
  subnets                          = "${var.subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}_recorder"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    recorder_queue_url  = "${module.recorder_queue.id}"
    metrics_namespace   = "${var.namespace}_recorder"
    message_bucket_name = "${var.messages_bucket}"

    vhs_recorder_dynamo_table_name = "${module.vhs_recorder.table_name}"
    vhs_recorder_bucket_name       = "${module.vhs_recorder.bucket_name}"

    sns_topic = "${module.recorded_works_topic.arn}"
  }

  env_vars_length = 6

  container_image   = "${var.recorder_container_image}"
  source_queue_name = "${module.recorder_queue.name}"
  source_queue_arn  = "${module.recorder_queue.arn}"
}

module "matcher" {
  source                           = "../service"
  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${var.cluster_name}"
  namespace_id                     = "${var.namespace_id}"
  subnets                          = "${var.subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}_matcher"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    queue_url               = "${module.matcher_queue.id}"
    metrics_namespace       = "${var.namespace}_matcher"
    vhs_bucket_name         = "${module.vhs_recorder.bucket_name}"
    topic_arn               = "${module.matched_works_topic.arn}"
    dynamo_table            = "${aws_dynamodb_table.matcher_graph_table.id}"
    dynamo_index            = "${var.matcher_graph_table_index}"
    dynamo_lock_table       = "${aws_dynamodb_table.matcher_lock_table.id}"
    dynamo_lock_table_index = "${var.matcher_lock_table_index}"
  }

  env_vars_length = 8

  container_image   = "${var.matcher_container_image}"
  source_queue_name = "${module.matcher_queue.name}"
  source_queue_arn  = "${module.matcher_queue.arn}"
}

module "merger" {
  source                           = "../service"
  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${var.cluster_name}"
  namespace_id                     = "${var.namespace_id}"
  subnets                          = "${var.subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}_merger"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    metrics_namespace        = "${var.namespace}_merger"
    messages_bucket_name     = "${var.messages_bucket}"
    topic_arn                = "${module.matched_works_topic.arn}"
    merger_queue_id          = "${module.merger_queue.id}"
    merger_topic_arn         = "${module.merged_works_topic.arn}"
    vhs_recorder_bucket_name = "${module.vhs_recorder.bucket_name}"
    vhs_recorder_table_name  = "${module.vhs_recorder.table_name}"
  }

  env_vars_length = 7

  container_image   = "${var.merger_container_image}"
  source_queue_name = "${module.merger_queue.name}"
  source_queue_arn  = "${module.merger_queue.arn}"
}

module "id_minter" {
  source = "../service"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${var.cluster_name}"
  namespace_id                     = "${var.namespace_id}"
  subnets                          = "${var.subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}_id_minter"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    metrics_namespace   = "${var.namespace}_id_minter"
    message_bucket_name = "${var.messages_bucket}"
    cluster_url         = "${var.identifiers_rds_cluster_host}"
    db_port             = "${var.identifiers_rds_cluster_port}"
    db_username         = "${var.identifiers_rds_cluster_username}"
    db_password         = "${var.identifiers_rds_cluster_password}"
    queue_url           = "${module.id_minter_queue.id}"
    topic_arn           = "${module.es_ingest_topic.arn}"
    max_connections     = 8
  }

  env_vars_length   = 9
  container_image   = "${var.id_minter_container_image}"
  source_queue_name = "${module.id_minter_queue.name}"
  source_queue_arn  = "${module.id_minter_queue.arn}"

  // Our RDS instance allows a maximum of 45 concurrent connections.
  // The maximum number of concurrent connection is determined by
  // max_connections * max_capacity so we always need to set those
  // two values in a way that their product doesn't exceed 45
  max_capacity = 3

  security_group_ids = ["${var.rds_access_security_group_id}"]
}

data "template_file" "es_cluster_host_ingestor" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_cluster_credentials["name"]}"
    region = "${var.es_cluster_credentials["region"]}"
  }
}

module "ingestor" {
  source = "../service"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${var.cluster_name}"
  namespace_id                     = "${var.namespace_id}"
  subnets                          = "${var.subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}_ingestor"
  aws_region                       = "${var.aws_region}"

  env_vars = {
    metrics_namespace   = "${var.namespace}_ingestor"
    message_bucket_name = "${var.messages_bucket}"
    es_host             = "${data.template_file.es_cluster_host_ingestor.rendered}"
    es_port             = "${var.es_cluster_credentials["port"]}"
    es_username         = "${var.es_cluster_credentials["username"]}"
    es_password         = "${var.es_cluster_credentials["password"]}"
    es_protocol         = "${var.es_cluster_credentials["protocol"]}"
    es_index            = "${var.index}"
    es_doc_type         = "work"
    ingest_queue_id     = "${module.es_ingest_queue.id}"
  }

  env_vars_length   = 11
  container_image   = "${var.ingestor_container_image}"
  source_queue_name = "${module.es_ingest_queue.name}"
  source_queue_arn  = "${module.es_ingest_queue.arn}"

  max_capacity = 5
}
