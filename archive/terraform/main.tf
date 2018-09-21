module "archivist" {
  source = "service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "${local.namespace}_archivist"
  aws_region                       = "${var.aws_region}"

  min_capacity = 1
  max_capacity = 1

  env_vars = {
    queue_url                   = "${module.archivist_queue.id}"
    archive_bucket              = "${aws_s3_bucket.archive_storage.id}"
    topic_arn                   = "${module.registrar_topic.arn}"
    archive_progress_table_name = "${aws_dynamodb_table.archive_progress_table.name}"
  }

  env_vars_length = 4

  container_image   = "${local.archivist_container_image}"
  source_queue_name = "${module.archivist_queue.name}"
  source_queue_arn  = "${module.archivist_queue.arn}"
}

module "registrar" {
  source = "service"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${local.private_subnets}"
  vpc_id                           = "${local.vpc_id}"
  service_name                     = "${local.namespace}_registrar"
  aws_region                       = "${var.aws_region}"

  min_capacity = 1
  max_capacity = 1

  env_vars = {
    queue_url                   = "${module.registrar_queue.id}"
    archive_bucket              = "${aws_s3_bucket.archive_storage.id}"
    topic_arn                   = "${module.registrar_completed_topic.arn}"
    vhs_bucket_name             = "${module.vhs_archive_manifest.bucket_name}"
    vhs_table_name              = "${module.vhs_archive_manifest.table_name}"
    archive_progress_table_name = "${aws_dynamodb_table.archive_progress_table.name}"
  }

  env_vars_length = 6

  container_image   = "${local.registrar_container_image}"
  source_queue_name = "${module.registrar_queue.name}"
  source_queue_arn  = "${module.registrar_queue.arn}"
}

module "api_ecs" {
  namespace = "${local.namespace}"
  source    = "api_ecs"

  api_path = "/storage/v1"

  nginx_container_image = "${local.nginx_services_ecs_container_image}"
  nginx_container_port  = "9000"

  archive_api_container_image = "${local.api_ecs_container_image}"
  archive_api_container_port  = "8888"

  archive_progress_table_name  = "${aws_dynamodb_table.archive_progress_table.name}"
  archive_ingest_sns_topic_arn = "${module.archivist_topic.arn}"

  vpc_id             = "${local.vpc_id}"
  private_subnets    = "${local.private_subnets}"
  public_subnets     = "${local.public_subnets}"
  certificate_domain = "api.wellcomecollection.org"
}
