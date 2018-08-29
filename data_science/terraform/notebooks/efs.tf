resource "aws_security_group" "efs_security_group" {
  name        = "${var.namespace}_efs_security_group"
  description = "Allow traffic between data science VMs and EFS"
  vpc_id      = "${var.vpc_id}"

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

  tags {
    Name = "${var.namespace}-efs"
  }
}

module "efs" {
  name                         = "notebooks-${var.namespace}_efs"
  source                       = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v1.0.0"
  vpc_id                       = "${var.vpc_id}"
  subnets                      = "${var.subnets}"
  efs_access_security_group_id = "${aws_security_group.efs_security_group.id}"
}
