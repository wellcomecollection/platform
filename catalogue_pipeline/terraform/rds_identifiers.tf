// TODO delete after we're confident the migration went smoothly
module "identifiers_rds_cluster" {
  source             = "git::https://github.com/wellcometrust/terraform.git//rds?ref=v1.0.5"
  cluster_identifier = "identifiers"
  database_name      = "identifiers"
  username           = "${var.rds_username}"
  password           = "${var.rds_password}"
  vpc_subnet_ids     = "${module.vpc_services.subnets}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"

  db_access_security_group = []

  vpc_security_group_ids = []
}

module "identifiers_delta_rds_cluster" {
  source             = "git::https://github.com/wellcometrust/terraform.git//rds?ref=v1.0.5"
  cluster_identifier = "identifiers-delta"
  database_name      = "identifiers"
  username           = "${var.rds_username}"
  password           = "${var.rds_password}"
  vpc_subnet_ids     = "${local.private_subnets}"
  vpc_id             = "${local.vpc_id}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"

  db_access_security_group = ["${module.catalogue_pipeline.rds_access_security_group_id}"]

  vpc_security_group_ids = []
}
