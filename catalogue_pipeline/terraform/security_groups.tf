module "service_egress_security_group" {
  source = "egress_security_group"

  name        = "catalogue_pipeline_service_egress"
  description = "Allow traffic between services"

  vpc_id     = "${local.vpc_id}"
  subnet_ids = "${local.private_subnets}"
}
