module "platform_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "platform-ecs-cluster"
  subnet_list           = ["${module.vpc_main.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_main_iam.instance_profile_name}"
  user_data             = "${data.template_file.platform_userdata.rendered}"
  vpc_id                = "${module.vpc_main.vpc_id}"
}

module "tools_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "tools-ecs-cluster"
  subnet_list           = ["${module.vpc_tools.subnets}"]
  key_name              = "${var.key_name}"
  instance_type         = "t2.medium"
  instance_profile_name = "${module.ecs_tools_iam.instance_profile_name}"
  user_data             = "${data.template_file.tools_userdata.rendered}"
  vpc_id                = "${module.vpc_tools.vpc_id}"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"
}
