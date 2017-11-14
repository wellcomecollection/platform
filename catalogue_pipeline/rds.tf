module "identifiers_rds_cluster" {
  source             = "git::https://github.com/wellcometrust/terraform.git//rds?ref=v1.0.5"
  cluster_identifier = "identifiers"
  database_name      = "identifiers"
  username           = "${var.rds_username}"
  password           = "${var.rds_password}"
  vpc_subnet_ids     = "${module.vpc_services.subnets}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"

  db_access_security_group = ["${module.services_cluster_asg.instance_sg_id}",
    "${module.services_cluster_asg_on_demand.instance_sg_id}",
  ]

  vpc_security_group_ids = [
    "${module.services_cluster_asg.instance_sg_id}",
  ]
}
