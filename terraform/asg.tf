module "services_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "service-cluster"
  subnet_list           = ["${module.vpc_services.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_services_iam.instance_profile_name}"
  user_data             = "${module.services_userdata.rendered}"
  vpc_id                = "${module.vpc_services.vpc_id}"
}

module "api_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "api-cluster"
  subnet_list           = ["${module.vpc_api.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_api_iam.instance_profile_name}"
  user_data             = "${module.api_userdata.rendered}"
  vpc_id                = "${module.vpc_api.vpc_id}"
}

module "tools_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "tools-cluster"
  subnet_list           = ["${module.vpc_tools.subnets}"]
  key_name              = "${var.key_name}"
  instance_type         = "t2.medium"
  instance_profile_name = "${module.ecs_tools_iam.instance_profile_name}"
  user_data             = "${module.tools_userdata.rendered}"
  vpc_id                = "${module.vpc_tools.vpc_id}"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"
}
