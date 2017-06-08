resource "aws_security_group" "database_sg" {
  description = "controls direct access to application instances"
  vpc_id      = "${var.vpc_id}"
  name        = "${var.database_name}_sg"

  ingress {
    protocol  = "tcp"
    from_port = 3306
    to_port   = 3306

    cidr_blocks = [
      "${var.admin_cidr_ingress}",
    ]
  }

  ingress {
    from_port = 3306
    to_port   = 3306
    protocol  = "tcp"

    security_groups = [
      "${var.db_access_security_group}",
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
