resource "aws_security_group" "external_lb_security_group" {
  name        = "${var.namespace}_external_lb_security_group"
  description = "Allow traffic between load balancer and internet"
  vpc_id      = "${var.vpc_id}"

  ingress {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443

    cidr_blocks = ["46.102.195.182/32", "195.143.129.132/32"]
  }

  ingress {
    protocol  = "tcp"
    from_port = 80
    to_port   = 80

    cidr_blocks = ["46.102.195.182/32", "195.143.129.132/32"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${var.namespace}"
  }
}

resource "aws_security_group" "service_lb_security_group" {
  name        = "${var.namespace}_service_lb_security_group"
  description = "Allow traffic between services and load balancer"
  vpc_id      = "${var.vpc_id}"

  ingress {
    protocol  = "tcp"
    from_port = "${var.nginx_container_port}"
    to_port   = "${var.nginx_container_port}"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${var.namespace}-service-lb"
  }
}