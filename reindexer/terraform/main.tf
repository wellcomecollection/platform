module "miro_reindexer" {
  source = "./reindex_worker"

  namespace      = "miro"
  vhs_table_name = "${local.vhs_miro_table_name}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name                 = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id                   = "${aws_ecs_cluster.cluster.id}"
  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}

module "sierra_reindexer" {
  source = "./reindex_worker"

  namespace      = "sierra"
  vhs_table_name = "${local.vhs_sierra_table_name}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name                 = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id                   = "${aws_ecs_cluster.cluster.id}"
  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}

module "sierra_items_reindexer" {
  source = "./reindex_worker"

  namespace      = "sierra_items"
  vhs_table_name = "${local.vhs_sierra_table_name}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"

  ecs_cluster_name                 = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id                   = "${aws_ecs_cluster.cluster.id}"
  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}
