module "catalogue_miro_reindexer" {
  source = "./reindex_worker"

  namespace                           = "catalogue_pipeline_miro"
  vhs_table_name                      = "${local.vhs_miro_table_name}"
  hybrid_records_topic_arn            = "${local.catalogue_miro_hybrid_records_topic_arn}"
  hybrid_records_topic_publish_policy = "${local.catalogue_miro_hybrid_records_topic_publish_policy}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.reindexer.id}"
}

module "catalogue_sierra_reindexer" {
  source = "./reindex_worker"

  namespace                           = "catalogue_pipeline_sierra"
  vhs_table_name                      = "${local.vhs_sierra_table_name}"
  hybrid_records_topic_arn            = "${local.catalogue_sierra_hybrid_records_topic_arn}"
  hybrid_records_topic_publish_policy = "${local.catalogue_sierra_hybrid_records_topic_publish_policy}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.reindexer.id}"
}

module "catalogue_sierra_items_reindexer" {
  source = "./reindex_worker"

  namespace                           = "catalogue_pipeline_sierra_items"
  vhs_table_name                      = "${local.vhs_sierra_items_table_name}"
  hybrid_records_topic_arn            = "${local.catalogue_sierra_items_hybrid_records_topic_arn}"
  hybrid_records_topic_publish_policy = "${local.catalogue_sierra_items_hybrid_records_topic_publish_policy}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.reindexer.id}"
}

module "reporting_miro_reindexer" {
  source = "./reindex_worker"

  namespace                           = "reporting_pipeline_miro"
  vhs_table_name                      = "${local.vhs_miro_table_name}"
  hybrid_records_topic_arn            = "${local.reporting_miro_hybrid_records_topic_arn}"
  hybrid_records_topic_publish_policy = "${local.reporting_miro_hybrid_records_topic_publish_policy}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.reindexer.id}"
}

module "reporting_sierra_reindexer" {
  source = "./reindex_worker"

  namespace                           = "reporting_pipeline_sierra"
  vhs_table_name                      = "${local.vhs_sierra_table_name}"
  hybrid_records_topic_arn            = "${local.reporting_sierra_hybrid_records_topic_arn}"
  hybrid_records_topic_publish_policy = "${local.reporting_sierra_hybrid_records_topic_publish_policy}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id   = "${aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.reindexer.id}"
}
