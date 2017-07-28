module "services_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "service-cluster"
  subnet_list           = ["${module.vpc_services.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_services_iam.instance_profile_name}"
  user_data             = "${module.services_userdata.rendered}"
  vpc_id                = "${module.vpc_services.vpc_id}"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"

  asg_desired = "4"
  asg_max     = "8"

  instance_type = "t2.small"

  sns_topic_arn         = "${module.ec2_terminating_topic.arn}"
  publish_to_sns_policy = "${module.ec2_terminating_topic.publish_policy}"
  alarm_topic_arn       = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}

module "monitoring_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "monitoring-cluster"
  subnet_list           = ["${module.vpc_monitoring.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_monitoring_iam.instance_profile_name}"
  user_data             = "${module.monitoring_userdata.rendered}"
  vpc_id                = "${module.vpc_monitoring.vpc_id}"

  instance_type = "t2.nano"

  admin_cidr_ingress    = "${var.admin_cidr_ingress}"
  sns_topic_arn         = "${module.ec2_terminating_topic.arn}"
  publish_to_sns_policy = "${module.ec2_terminating_topic.publish_policy}"
  alarm_topic_arn       = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}

module "api_cluster_asg" {
  source                = "./ecs_asg"
  asg_name              = "api-cluster"
  subnet_list           = ["${module.vpc_api.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_api_iam.instance_profile_name}"
  user_data             = "${module.api_userdata.rendered}"
  vpc_id                = "${module.vpc_api.vpc_id}"

  asg_desired = "3"
  asg_max     = "6"

  instance_type = "t2.medium"

  sns_topic_arn         = "${module.ec2_terminating_topic.arn}"
  publish_to_sns_policy = "${module.ec2_terminating_topic.publish_policy}"
  alarm_topic_arn       = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}
