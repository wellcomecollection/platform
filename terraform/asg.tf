module "services_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "service-cluster"
  subnet_list           = ["${module.vpc_services.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_services_iam.instance_profile_name}"
  user_data             = "${module.services_userdata.rendered}"
  vpc_id                = "${module.vpc_services.vpc_id}"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"
  asg_desired           = "3"
  asg_max               = "4"
  instance_type         = "t2.large"
}

module "monitoring_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "monitoring-cluster"
  subnet_list           = ["${module.vpc_monitoring.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_monitoring_iam.instance_profile_name}"
  user_data             = "${module.monitoring_userdata.rendered}"
  vpc_id                = "${module.vpc_monitoring.vpc_id}"
  instance_type         = "t2.medium"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"

  # Grafana containers persist information about configured dashboards on a volume exposed by the EC2
  # instance they are running on. Mounted volumes are not synchronized across different EC2 instances of an
  # ECS cluster. If we have more than one EC2 instance we cannot guarantee that the files will be where
  # they need to be or that they will be up to date
  asg_max = "1"
}

module "api_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "api-cluster"
  subnet_list           = ["${module.vpc_api.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_api_iam.instance_profile_name}"
  user_data             = "${module.api_userdata.rendered}"
  vpc_id                = "${module.vpc_api.vpc_id}"
  instance_type         = "t2.large"
}
