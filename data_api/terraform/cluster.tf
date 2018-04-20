module "data_api_cluster" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/cluster?ref=v8.1.0"
  name   = "data-api"

  vpc_subnets = ["${module.vpc_data_api.subnets}"]
  vpc_id      = "${module.vpc_data_api.vpc_id}"

  alb_certificate_domain = "data.wellcomecollection.org"

  key_name = "${var.key_name}"

  ec2_terminating_topic_arn                       = "${local.ec2_terminating_topic_arn}"
  ec2_terminating_topic_publish_policy            = "${local.ec2_terminating_topic_publish_policy}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"

  alb_log_bucket_id = "${local.bucket_alb_logs_id}"

  asg_spot_max = "10"
}
