module "service_egress_security_group" {
  source = "../../../terraform-modules/egress_security_group"

  name        = "${var.namespace}_service_egress"
  description = "Allow traffic between services"

  vpc_id      = "${var.vpc_id}"
  subnet_ids  = "${var.subnets}"
}
