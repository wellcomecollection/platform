locals {
  base_security_groups = ["${aws_security_group.full_egress.id}"]

  instance_security_groups = "${concat(local.base_security_groups, aws_security_group.ssh_controlled_ingress.*.id, var.custom_security_groups)}"
}

resource "aws_security_group" "ssh_controlled_ingress" {
  description = "controls SSH access to application instances"
  vpc_id      = "${var.vpc_id}"
  name        = "${var.name}_ssh_controlled_ingress_${random_id.sg_append.hex}"

  # If there aren't any CIDR blocks or security groups that this rule
  # applies to, we can skip creating the security group.
  count = "${length(var.controlled_access_cidr_ingress) + length(var.controlled_access_security_groups) > 0 ? 1 : 0}"

  ingress {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22

    cidr_blocks     = ["${var.controlled_access_cidr_ingress}"]
    security_groups = ["${var.controlled_access_security_groups}"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "full_egress" {
  description = "controls direct access to application instances"
  vpc_id      = "${var.vpc_id}"
  name        = "${var.name}_full_egress_${random_id.sg_append.hex}"

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "random_id" "sg_append" {
  keepers = {
    sg_id = "${var.name}"
  }

  byte_length = 8
}
