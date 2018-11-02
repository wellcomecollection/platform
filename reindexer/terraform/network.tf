resource "aws_service_discovery_private_dns_namespace" "reindexer" {
  name = "reindexer"
  vpc  = "${local.vpc_id}"
}
