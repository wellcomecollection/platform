resource "aws_ecs_cluster" "cluster" {
  name = "${local.namespace}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${local.namespace}"
  vpc  = "${local.vpc_id}"
}
