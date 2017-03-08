resource "aws_security_group" "lb_sg" {
  description = "controls access to the application ELB"

  vpc_id = "${module.vpc_main.vpc_id}"
  name   = "tf-ecs-lbsg"

  ingress {
    protocol    = "tcp"
    from_port   = 443
    to_port     = 443
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }
}

resource "aws_security_group" "tools_lb_sg" {
  description = "controls access to the application ELB"

  vpc_id = "${module.vpc_tools.vpc_id}"
  name   = "tf-ecs-tools-lbsg"

  ingress {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443

    cidr_blocks = [
      "${var.admin_cidr_ingress}",
    ]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }
}

resource "aws_security_group" "instance_sg" {
  description = "controls direct access to application instances"
  vpc_id      = "${module.vpc_main.vpc_id}"
  name        = "tf-ecs-instsg"

  ingress {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22

    cidr_blocks = [
      "${var.admin_cidr_ingress}",
    ]
  }

  ingress {
    from_port = 32768
    to_port   = 61000
    protocol  = "tcp"

    security_groups = [
      "${aws_security_group.lb_sg.id}",
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "tools_instance_sg" {
  description = "controls direct access to application instances"
  vpc_id      = "${module.vpc_tools.vpc_id}"
  name        = "tf-ecs-tools-instsg"

  ingress {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22

    cidr_blocks = [
      "${var.admin_cidr_ingress}",
    ]
  }

  ingress {
    from_port = 32768
    to_port   = 61000
    protocol  = "tcp"

    security_groups = [
      "${aws_security_group.tools_lb_sg.id}",
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "efs_mnt_sg" {
  name        = "efs-mnt"
  description = "Allow traffic from instances"
  vpc_id      = "${module.vpc_tools.vpc_id}"

  ingress {
    from_port = 2049
    to_port   = 2049
    protocol  = "tcp"

    security_groups = [
      "${aws_security_group.tools_instance_sg.id}",
    ]
  }
}
