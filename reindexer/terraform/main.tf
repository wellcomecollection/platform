data "aws_dynamodb_table" "miro" {
  name = "${local.vhs_miro_table_name}"
}

module "miro_reindexer" {
  source = "./reindex_worker"

  namespace     = "miro"
  vhs_table_arn = "${data.aws_dynamodb_table.miro.arn}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"


  ecs_cluster_name                 = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id                   = "${aws_ecs_cluster.cluster.id}"
  vpc_id                           = "${local.vpc_id}"
  private_subnets                  = "${local.private_subnets}"
  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}

data "aws_dynamodb_table" "sierra" {
  name = "${local.vhs_sierra_table_name}"
}

module "sierra_reindexer" {
  source = "./reindex_worker"

  namespace     = "sierra"
  vhs_table_arn = "${data.aws_dynamodb_table.sierra.arn}"

  reindex_worker_container_image = "${local.reindex_worker_container_image}"


  ecs_cluster_name                 = "${aws_ecs_cluster.cluster.name}"
  ecs_cluster_id                   = "${aws_ecs_cluster.cluster.id}"
  vpc_id                           = "${local.vpc_id}"
  private_subnets                  = "${local.private_subnets}"
  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
}
