resource "aws_vpc_endpoint" "s3" {
  vpc_id       = "${local.vpc_id}"
  service_name = "com.amazonaws.${var.aws_region}.s3"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${local.namespace}"
  vpc  = "${local.vpc_id}"
}