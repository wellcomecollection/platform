module "grafana_efs" {
  name                         = "grafana"
  source                       = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v1.0.0"
  vpc_id                       = "${local.vpc_id}"
  subnets                      = "${local.private_subnets}"
  efs_access_security_group_id = "${aws_security_group.efs_security_group.id}"
}

resource "aws_security_group" "efs_security_group" {
  name        = "${local.namespace}_efs_security_group"
  description = "Allow traffic between services and efs"
  vpc_id      = "${local.vpc_id}"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.namespace}-efs"
  }
}
