module "egress_security_group" {
  source = "github.com/wellcometrust/terraform//network/prebuilt/vpc/egress_security_group?ref=bc8f95f"

  name = "${var.namespace}"

  vpc_id     = "${local.vpc_id}"
  subnet_ids = "${local.private_subnets}"
}

resource "aws_security_group" "interservice_security_group" {
  name        = "${var.namespace}_interservice_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id}"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  tags {
    Name = "${var.namespace}-interservice"
  }
}
