module "appautoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/app/ecs?ref=v1.1.0"
  name   = "sierra_${var.resource_type}_merger"

  cluster_name = "${var.cluster_name}"
  service_name = "${module.sierra_merger_service.service_name}"
}

module "sqs_autoscaling_alarms" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/alarms/sqs?ref=v1.1.0"
  name   = "sierra_${var.resource_type}_merger"

  queue_name = "${module.update_events_queue.name}"

  scale_up_arn   = "${module.appautoscaling.scale_up_arn}"
  scale_down_arn = "${module.appautoscaling.scale_down_arn}"
}

module "sierra_merger_service" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.3.0"
  name               = "sierra_${var.resource_type}_merger"
  cluster_id         = "${var.cluster_id}"
  task_role_arn      = "${module.ecs_sierra_merger.task_role_arn}"
  vpc_id             = "${var.vpc_id}"
  app_uri            = "${var.ecr_repository_url}:${var.release_id}"
  listener_https_arn = "${var.alb_listener_https_arn}"
  listener_http_arn  = "${var.alb_listener_http_arn}"
  path_pattern       = "/sierra_to_dynamo/${var.resource_type}/*"
  alb_priority       = "${var.alb_priority}"
  healthcheck_path   = "/sierra_to_dynamo/${var.resource_type}/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"

  config_key           = "config/${var.build_env}/sierra_${var.resource_type}_merger.ini"
  config_template_path = "config/sierra_merger.ini.template"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    windows_queue_url = "${module.update_events_queue.id}"
    metrics_namespace = "sierra_${var.resource_type}_merger"
    dynamo_table_name = "${var.target_dynamo_table_name}"
  }

  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"
  server_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"
}
