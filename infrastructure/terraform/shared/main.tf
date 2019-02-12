module "ecs_ec2_instance_tagger" {
  source = "ecs_ec2_instance_tagger"

  ecs_container_instance_state_change_name = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.name}"
  ecs_container_instance_state_change_arn  = "${aws_cloudwatch_event_rule.ecs_container_instance_state_change.arn}"

  lambda_error_alarm_arn = "${module.lambda_error_alarm.arn}"

  infra_bucket = "${local.infra_bucket}"
}
