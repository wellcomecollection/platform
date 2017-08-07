resource "aws_security_group" "instance_sg" {
  description = "controls direct access to batch instances"
  vpc_id      = "${var.vpc_id}"
  name        = "${var.name}_batch_instance_sg"

  ingress {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22

    cidr_blocks = [
      "${var.admin_cidr_ingress}",
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
