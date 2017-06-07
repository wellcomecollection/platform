module "identifiers_rds_cluster" {
  source             = "./rds"
  cluster_identifier = "identifiers"
  database_name      = "identifiers"
  username           = "${var.rds_username}"
  password           = "${var.rds_password}"
  vpc_subnet_ids     = "${module.vpc_services.subnets}"

  vpc_security_group_ids = [
    "${module.services_cluster_asg.instance_sg_id}",
  ]
}
