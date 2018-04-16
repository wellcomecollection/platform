module "loris_cluster_asg" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "loris-cluster"
  subnet_list           = ["${module.vpc_loris.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_loris_iam.instance_profile_name}"
  user_data             = "${module.loris_userdata.rendered}"
  vpc_id                = "${module.vpc_loris.vpc_id}"

  asg_desired = "4"
  asg_max     = "4"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "c4.xlarge"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"

  ebs_device_name = "/dev/xvdb"
  ebs_size        = 180
  ebs_volume_type = "gp2"
}
