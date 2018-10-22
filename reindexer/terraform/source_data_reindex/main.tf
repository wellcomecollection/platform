module "miro_reindexer" {
  source = "../reindex_worker"

  namespace      = "${var.namespace}_miro"
  vhs_table_name = "${local.vhs_miro_table_name}"

  reindex_worker_container_image = "${var.reindex_worker_container_image}"

  ecs_cluster_name = "${data.aws_ecs_cluster.cluster.cluster_name}"
  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}

module "sierra_reindexer" {
  source = "../reindex_worker"

  namespace      = "${var.namespace}_sierra"
  vhs_table_name = "${local.vhs_sierra_table_name}"

  reindex_worker_container_image = "${var.reindex_worker_container_image}"

  ecs_cluster_name = "${data.aws_ecs_cluster.cluster.cluster_name}"
  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}

module "sierra_items_reindexer" {
  source = "../reindex_worker"

  namespace      = "${var.namespace}_sierra_items"
  vhs_table_name = "${local.vhs_sierra_items_table_name}"

  reindex_worker_container_image = "${var.reindex_worker_container_image}"

  ecs_cluster_name = "${data.aws_ecs_cluster.cluster.cluster_name}"
  ecs_cluster_id   = "${data.aws_ecs_cluster.cluster.id}"

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}
