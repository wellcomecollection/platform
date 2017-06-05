module "services_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "service-cluster"
  subnet_list           = ["${module.vpc_services.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_services_iam.instance_profile_name}"
  user_data             = "${module.services_userdata.rendered}"
  vpc_id                = "${module.vpc_services.vpc_id}"
  asg_desired           = "3"
  asg_max               = "4"
  instance_type         = "t2.large"
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
