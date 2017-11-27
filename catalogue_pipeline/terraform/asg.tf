module "services_cluster_asg" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "service-cluster"
  subnet_list           = ["${module.vpc_services.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_services_iam.instance_profile_name}"
  user_data             = "${module.services_userdata.rendered}"
  vpc_id                = "${module.vpc_services.vpc_id}"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"

  asg_min     = "0"
  asg_desired = "2"
  asg_max     = "4"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "m4.xlarge"

  use_spot   = 1
  spot_price = "0.1"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"
}

module "services_cluster_asg_on_demand" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "service-cluster-on-demand"
  subnet_list           = ["${module.vpc_services.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_services_iam.instance_profile_name}"
  user_data             = "${module.services_userdata.rendered}"
  vpc_id                = "${module.vpc_services.vpc_id}"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"

  asg_min     = "0"
  asg_desired = "0"
  asg_max     = "1"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "m4.xlarge"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"
}
