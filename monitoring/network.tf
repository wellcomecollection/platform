module "network" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v11.0.0"
  name       = "${local.namespace}"
  cidr_block = "${local.vpc_cidr_block}"
  az_count   = "2"
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id       = "${module.network.vpc_id}"
  service_name = "com.amazonaws.${var.aws_region}.s3"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${local.namespace}"
  vpc  = "${module.network.vpc_id}"
}

resource "aws_ecs_cluster" "monitoring" {
  name = "monitoring_cluster"
}
fi