module "network" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=ecs_v2"
  name       = "sierra_adapter"
  cidr_block = "${var.vps_cidr_block}"
  az_count   = "3"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}"
  vpc  = "${module.network.vpc_id}"
}