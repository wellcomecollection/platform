data "aws_ssm_parameter" "rds_username" {
  name = "/aws/reference/secretsmanager/catalogue/id_minter/rds_user"
}

data "aws_ssm_parameter" "rds_password" {
  name = "/aws/reference/secretsmanager/catalogue/id_minter/rds_password"
}

locals {
  rds_username = "${data.aws_ssm_parameter.rds_username.value}"
  rds_password = "${data.aws_ssm_parameter.rds_password.value}"
}

module "identifiers_rds_cluster" {
  source             = "git::https://github.com/wellcometrust/terraform.git//rds?ref=v13.0.0"
  cluster_identifier = "identifiers"
  database_name      = "identifiers"
  username           = "${local.rds_username}"
  password           = "${local.rds_password}"
  vpc_subnet_ids     = "${local.private_subnets_new}"
  vpc_id             = "${local.vpc_id_new}"
  admin_cidr_ingress = "${local.admin_cidr_ingress}"

  db_access_security_group = ["${aws_security_group.rds_ingress_security_group.id}"]

  vpc_security_group_ids = []
}

resource "aws_security_group" "rds_ingress_security_group" {
  name        = "pipeline_rds_ingress_security_group"
  description = "Allow traffic to rds database"
  vpc_id      = "${local.vpc_id_new}"

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
