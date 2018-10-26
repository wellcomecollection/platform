resource "aws_service_discovery_private_dns_namespace" "catalogue_namespace" {
  name = "reindexer_catalogue_pipeline"
  vpc  = "${local.vpc_id}"
}

resource "aws_service_discovery_private_dns_namespace" "reporting_namespace" {
  name = "reindexer_reporting_pipeline"
  vpc  = "${local.vpc_id}"
}
