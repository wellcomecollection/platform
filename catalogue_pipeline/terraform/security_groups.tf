module "service_egress_security_group" {
  source = "egress_security_group"

  name        = "catalogue_pipeline_service_egress"
  description = "Allow traffic between services"

  vpc_id     = "${local.vpc_id}"
  subnet_ids = "${local.private_subnets}"
}

module "egress_security_group" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/egress_security_group?ref=c71887b"

  name        = "catalogue_pipeline_services"

  vpc_id     = "${local.vpc_id}"
  subnet_ids = "${local.private_subnets}"
}
