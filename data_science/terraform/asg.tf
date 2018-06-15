module "data_science_experiments_cluster_asg" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "data-science-experiments-cluster"
  subnet_list           = ["${module.vpc.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_data_science_experiments_iam.instance_profile_name}"
  user_data             = "${module.data_science_experiments_userdata.rendered}"
  vpc_id                = "${module.vpc.vpc_id}"

  asg_desired = "1"
  asg_max     = "1"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "t2.micro"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"
}
