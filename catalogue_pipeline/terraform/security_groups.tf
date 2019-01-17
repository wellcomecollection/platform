module "egress_security_group" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/egress_security_group?ref=v19.5.0"

  name = "catalogue_pipeline_services"

  vpc_id     = "${local.vpc_id}"
  subnet_ids = "${local.private_subnets}"
}
