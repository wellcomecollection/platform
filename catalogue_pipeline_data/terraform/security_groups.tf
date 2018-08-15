resource "aws_security_group" "rds_access_security_group" {
  name        = "pipeline_rds_access_security_group"
  description = "Allow traffic to rds database"
  vpc_id      = "${local.vpc_id}"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  tags {
    Name = "pipeline-rds-access"
  }
}
