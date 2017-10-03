module "cloudformation_stack" {
  source = "ecs_asg_cf_stack"

  publish_to_sns_policy = "${var.publish_to_sns_policy}"
  alarm_topic_arn       = "${var.alarm_topic_arn}"
  sns_topic_arn         = "${var.sns_topic_arn}"
  subnet_list           = "${var.subnet_list}"
  asg_name              = "${var.asg_name}"
  launch_config_name    = "${module.launch_config.name}"

  asg_max     = "${var.asg_max}"
  asg_desired = "${var.asg_desired}"
  asg_min     = "${var.asg_min}"
}

module "launch_config" {
  source = "ecs_asg_launch_config"

  instance_profile_name = "${var.instance_profile_name}"
  vpc_id                = "${var.vpc_id}"
  asg_name              = "${var.asg_name}"
  image_id              = "${var.image_id}"
  user_data             = "${var.user_data}"
  key_name              = "${var.key_name}"
  use_spot              = "${var.use_spot}"
  spot_price            = "${var.spot_price}"
  instance_type         = "${var.instance_type}"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"
  ebs_device_name       = "${var.ebs_device_name}"
  ebs_size              = "${var.ebs_size}"
  public_ip             = "${var.public_ip}"
  ebs_volume_type       = "${var.ebs_volume_type}"
  ebs_iops              = "${var.ebs_iops}"
}
