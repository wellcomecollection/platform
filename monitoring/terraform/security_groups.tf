resource "aws_security_group" "service_egress_security_group" {
  name        = "${local.namespace}_service_egress_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id}"

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-egress"
  }
}

resource "aws_security_group" "service_lb_security_group" {
  name        = "${local.namespace}_service_lb_security_group"
  description = "Allow traffic between services and load balancer"
  vpc_id      = "${local.vpc_id}"

  ingress {
    protocol  = "tcp"
    from_port = 3000
    to_port   = 3000
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-service-lb"
  }
}

resource "aws_security_group" "external_lb_security_group" {
  name        = "${local.namespace}_external_lb_security_group"
  description = "Allow traffic between load balancer and internet"
  vpc_id      = "${local.vpc_id}"

  ingress {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443

    cidr_blocks = ["${var.admin_cidr_ingress}"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-external-lb"
  }
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

  tags {
    Name = "${local.namespace}-efs"
  }
}
