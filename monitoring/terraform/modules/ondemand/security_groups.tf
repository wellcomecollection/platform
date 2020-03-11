locals {
  base_security_groups = [aws_security_group.full_egress.id]

  instance_security_groups = concat(
    local.base_security_groups,
    var.custom_security_groups
  )
}

resource "aws_security_group" "full_egress" {
  # TODO: This description has been copy/pasted from a previous security group.
  # Don't let it propogate to new stacks!
  description = "controls direct access to application instances"
  vpc_id      = var.vpc_id
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
    sg_id = var.name
  }

  byte_length = 8
}
