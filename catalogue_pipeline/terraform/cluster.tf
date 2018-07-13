module "catalogue_pipeline_cluster" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/cluster?ref=v6.4.1"
  name   = "catalogue-pipeline"

  vpc_subnets = ["${module.vpc_services.subnets}"]
  vpc_id      = "${module.vpc_services.vpc_id}"

  alb_certificate_domain = "catalogue-pipeline.wellcomecollection.org"

  key_name = "${var.key_name}"

  ec2_terminating_topic_arn                       = "${local.ec2_terminating_topic_arn}"
  ec2_terminating_topic_publish_policy            = "${local.ec2_terminating_topic_publish_policy}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"

  alb_log_bucket_id = "${local.bucket_alb_logs_id}"

  asg_spot_max = "10"
}
