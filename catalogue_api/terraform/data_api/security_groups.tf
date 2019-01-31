resource "aws_security_group" "service_egress_security_group" {
  name        = "${var.namespace}-service_egress_security_group"
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
