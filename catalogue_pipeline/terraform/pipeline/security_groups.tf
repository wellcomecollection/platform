resource "aws_security_group" "service_egress_security_group" {
  name        = "${var.namespace}_service_egress_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${var.vpc_id}"

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${var.namespace}-egress"
  }
}

data "aws_vpc_endpoint_service" "sns" {
  service = "sns"
}

resource "aws_vpc_endpoint" "sns" {
  vpc_id            = "${var.vpc_id}"
  vpc_endpoint_type = "Interface"

  security_group_ids = [
    "${aws_security_group.service_egress_security_group.id}"
  ]

  service_name      = "${data.aws_vpc_endpoint_service.sns.service_name}"
}
