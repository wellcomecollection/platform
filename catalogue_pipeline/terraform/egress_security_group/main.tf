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

data "aws_vpc_endpoint_service" "events" {
  service = "events"
}

resource "aws_vpc_endpoint" "events" {
  vpc_id            = "${var.vpc_id}"
  vpc_endpoint_type = "Interface"

  security_group_ids = [
    "${aws_security_group.security_group.id}",
  ]

  subnet_ids = ["${var.subnet_ids}"]

  service_name = "${data.aws_vpc_endpoint_service.events.service_name}"
  depends_on   = ["aws_security_group.security_group"]
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

  depends_on = ["aws_security_group.security_group"]

  subnet_ids = ["${var.subnet_ids}"]

  service_name = "${data.aws_vpc_endpoint_service.logs.service_name}"
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

  depends_on = ["aws_security_group.security_group"]

  subnet_ids = ["${var.subnet_ids}"]

  service_name = "${data.aws_vpc_endpoint_service.sns.service_name}"
}
