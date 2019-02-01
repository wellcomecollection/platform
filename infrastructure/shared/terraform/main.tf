module "drain_ecs_container_instance" {
  source = "drain_ecs_container_instance"

  ec2_terminating_topic_publish_policy = "${module.ec2_terminating_topic.publish_policy}"
  ec2_terminating_topic_arn            = "${module.ec2_terminating_topic.arn}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"

  infra_bucket = "${local.infra_bucket}"
}

module "ecs_ec2_instance_tagger" {
  source = "ecs_ec2_instance_tagger"

  ecs_container_instance_state_change_name = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.name}"
  ecs_container_instance_state_change_arn  = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.arn}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"

  infra_bucket = "${local.infra_bucket}"
}
