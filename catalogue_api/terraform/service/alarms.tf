locals {
  healthy_host_threshold = "${var.deployment_minimum_healthy_percent * var.task_desired_count / 100.0}"
}

module "alb_alarms" {
  source           = "git::https://github.com/wellcometrust/terraform.git//cloudwatch/prebuilt/alb_alarms?ref=load-balanced-alarms"
  enable_alb_alarm = "${var.enable_alb_alarm}"

  service_name                 = "${var.name}"
  server_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"
  target_group_id              = "${module.service.target_group_arn_suffix}"
  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"

  healthy_host_threshold = "${local.healthy_host_threshold}"
}
