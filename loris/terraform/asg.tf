module "loris_cluster_asg" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "loris-cluster"
  subnet_list           = ["${local.vpc_api_subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_loris_iam.instance_profile_name}"
  user_data             = "${module.loris_userdata.rendered}"
  vpc_id                = "${local.vpc_api_id}"

  asg_desired = "2"
  asg_max     = "4"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "t2.xlarge"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"
}

module "loris_cluster_asg_ebs" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "loris-cluster-ebs"
  subnet_list           = ["${local.vpc_api_subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_loris_iam.instance_profile_name}"
  user_data             = "${module.loris_userdata_ebs.rendered}"
  vpc_id                = "${local.vpc_api_id}"

  asg_desired = "2"
  asg_max     = "4"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "c4.xlarge"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"

  ebs_device_name = "/dev/xvdb"
  ebs_size        = 180
  ebs_volume_type = "io1"
  ebs_iops        = "2000"
}

module "loris_cluster_asg_ebs_large" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "loris-cluster-ebs-large"
  subnet_list           = ["${local.vpc_api_subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_loris_iam.instance_profile_name}"
  user_data             = "${module.loris_userdata_ebs_large.rendered}"
  vpc_id                = "${local.vpc_api_id}"

  asg_desired = "2"
  asg_max     = "4"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "c4.large"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"

  ebs_device_name = "/dev/xvdb"
  ebs_size        = 180
  ebs_volume_type = "io1"
  ebs_iops        = "2000"
}
