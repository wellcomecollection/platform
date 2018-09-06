resource "aws_security_group" "rds_access_security_group" {
  name        = "pipeline_rds_access_security_group"
  description = "Allow traffic to rds database"
  subnet_ids  = "${local.private_subnets}"

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
