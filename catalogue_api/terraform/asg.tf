module "api_cluster_asg" {
  source                = "git::https://github.com/wellcometrust/terraform.git//ecs_asg?ref=v1.0.0"
  asg_name              = "api-cluster"
  subnet_list           = ["${module.vpc_api.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_api_iam.instance_profile_name}"
  user_data             = "${module.api_userdata.rendered}"
  vpc_id                = "${module.vpc_api.vpc_id}"

  asg_desired = "2"
  asg_max     = "4"

  image_id      = "${data.aws_ami.stable_coreos.id}"
  instance_type = "t2.xlarge"

  sns_topic_arn         = "${local.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${local.ec2_terminating_topic_publish_policy}"

  alarm_topic_arn = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"
}
