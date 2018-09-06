resource "aws_security_group" "security_group" {
  name        = "${var.name}"
  description = "${var.description}"
  vpc_id      = "${var.vpc_id}"

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${var.name}"
  }
}

data "aws_vpc_endpoint_service" "logs" {
  service = "logs"
}

resource "aws_vpc_endpoint" "logs" {
  vpc_id            = "${var.vpc_id}"
  vpc_endpoint_type = "Interface"

  security_group_ids = [
    "${aws_security_group.security_group.id}",
  ]

  service_name = "${data.aws_vpc_endpoint_service.logs.service_name}"
}

resource "aws_vpc_endpoint_route_table_association" "private_logs" {
  count = "${length(var.private_route_table_ids)}"

  vpc_endpoint_id = "${aws_vpc_endpoint.logs.id}"
  route_table_id  = "${var.private_route_table_ids[count.index]}"
}

data "aws_vpc_endpoint_service" "sns" {
  service = "sns"
}

resource "aws_vpc_endpoint" "sns" {
  vpc_id            = "${var.vpc_id}"
  vpc_endpoint_type = "Interface"

  security_group_ids = [
    "${aws_security_group.security_group.id}",
  ]

  service_name = "${data.aws_vpc_endpoint_service.sns.service_name}"
}

resource "aws_vpc_endpoint_route_table_association" "private_sns" {
  count = "${length(var.private_route_table_ids)}"

  vpc_endpoint_id = "${aws_vpc_endpoint.sns.id}"
  route_table_id  = "${var.private_route_table_ids[count.index]}"
}
