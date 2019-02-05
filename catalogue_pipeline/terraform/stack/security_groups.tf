module "egress_security_group" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/egress_security_group?ref=v19.5.0"

  name = "${var.namespace}_pipeline_services"

  vpc_id     = "${var.vpc_id}"
  subnet_ids = "${var.subnets}"
}
