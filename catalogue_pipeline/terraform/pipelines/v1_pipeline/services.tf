module "id_minter" {
  source = "../service"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"
  cluster_name                     = "${var.cluster_name}"
  cluster_id                       = "${var.cluster_id}"
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
  max_capacity = 2

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
  cluster_id                       = "${var.cluster_id}"
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

  env_vars_length   = 10
  container_image   = "${var.ingestor_container_image}"
  source_queue_name = "${module.es_ingest_queue.name}"
  source_queue_arn  = "${module.es_ingest_queue.arn}"

  max_capacity = 3
}
