resource "aws_security_group" "service_egress_security_group" {
  name        = "${local.namespace}_service_egress_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id}"

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"

    cidr_blocks = [
      "0.0.0.0/0",
    ]
  }

  tags {
    Name = "${local.namespace}-egress"
  }
}

resource "aws_security_group" "interservice_security_group" {
  name        = "archive_interservice_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${local.vpc_id}"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  tags {
    Name = "${local.namespace}-interservice"
  }
}

data "aws_subnet" "private" {
  count = "${length(local.private_subnets)}"
  id = "${element(local.private_subnets, count.index)}"
}

resource "aws_security_group" "tcp_access_security_group" {
  name        = "archive_nlb_security_group"
  description = "Allow traffic between load balancer and internet"
  vpc_id      = "${local.vpc_id}"

  ingress {
    protocol    = "tcp"
    from_port   = 9001
    to_port     = 9001
    cidr_blocks = ["${data.aws_subnet.private.*.cidr_block}"]
  }
}
