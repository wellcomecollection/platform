module "sqs_autoscaling_alarms" {
  source = "./alarms_sqs"
  name   = "${module.service.service_name}"

  queue_name = "${module.reindex_worker_queue.name}"

  scale_up_arn   = "${module.service.scale_up_arn}"
  scale_down_arn = "${module.service.scale_down_arn}"

  high_period_in_minutes = "${var.scale_up_period_in_minutes}"
  low_period_in_minutes  = "${var.scale_down_period_in_minutes}"
}
