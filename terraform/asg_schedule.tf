/*
  The monitoring cluster isn't accessible outside the office, so running it
  outside working hours is just wasting money.  Have it turn itself off when
  we're not working.
*/

resource "aws_autoscaling_schedule" "turn_off_monitoring" {
  scheduled_action_name  = "Turn off the monitoring cluster outside working hours"
  desired_capacity       = 0
  max_size               = 0
  recurrence             = "0 19 * * 1-5"
  autoscaling_group_name = "${module.monitoring_cluster_asg.asg_name}"
}

resource "aws_autoscaling_schedule" "turn_on_monitoring" {
  scheduled_action_name  = "Turn on the monitoring cluster during working hours"
  desired_capacity       = "${module.services_cluster_asg.asg_desired}"
  max_size               = "${module.monitoring_cluster_asg.asg_max}"
  recurrence             = "0 08 * * 1-5"
  autoscaling_group_name = "${module.monitoring_cluster_asg.asg_name}"
}

/*
  The services cluster needs extra capacity when we're doing deployments,
  but deployments usually only happen during working hours.  Reduce the slack
  when we're not working.
*/

resource "aws_autoscaling_schedule" "reduce_services" {
  scheduled_action_name  = "Turn off the services cluster outside working hours"
  desired_capacity       = 0
  max_size               = 0
  recurrence             = "0 19 * * 1-5"
  autoscaling_group_name = "${module.services_cluster_asg.asg_name}"
}

resource "aws_autoscaling_schedule" "increase_services" {
  scheduled_action_name  = "Turn on the services cluster during working hours"
  desired_capacity       = "${module.services_cluster_asg.asg_desired}"
  max_size               = "${module.services_cluster_asg.asg_max}"
  recurrence             = "0 08 * * 1-5"
  autoscaling_group_name = "${module.services_cluster_asg.asg_name}"
}
