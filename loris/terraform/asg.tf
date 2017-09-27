module "loris_cluster_asg" {
  source                = "../../terraform/ecs_asg"
  asg_name              = "loris-cluster"
  subnet_list           = ["${data.terraform_remote_state.platform.vpc_api_subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_loris_iam.instance_profile_name}"
  user_data             = "${module.loris_userdata.rendered}"
  vpc_id                = "${data.terraform_remote_state.platform.vpc_api_id}"

  asg_desired = "2"
  asg_max     = "4"

  image_id      = "${data.terraform_remote_state.platform.ecs_ami_id}"
  instance_type = "t2.xlarge"

  sns_topic_arn         = "${data.terraform_remote_state.platform.ec2_terminating_topic_arn}"
  publish_to_sns_policy = "${data.terraform_remote_state.platform.ec2_terminating_topic_publish_policy}"
  alarm_topic_arn       = "${data.terraform_remote_state.platform.ec2_instance_terminating_for_too_long_alarm_arn}"
}
