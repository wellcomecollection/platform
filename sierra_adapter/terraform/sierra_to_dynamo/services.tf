module "appautoscaling" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/app/ecs?ref=v1.1.0"
  name   = "sierra_to_dynamo_${var.resource_type}"

  cluster_name = "${var.cluster_name}"
  service_name = "${module.sierra_to_dynamo_service.service_name}"
}

module "sqs_autoscaling_alarms" {
  source = "git::https://github.com/wellcometrust/terraform.git//autoscaling/alarms/sqs?ref=v1.1.0"
  name   = "sierra_to_dynamo_${var.resource_type}"

  queue_name = "${module.windows_queue.name}"

  scale_up_arn   = "${module.appautoscaling.scale_up_arn}"
  scale_down_arn = "${module.appautoscaling.scale_down_arn}"
}

module "sierra_to_dynamo_service" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "sierra_to_dynamo_${var.resource_type}"
  cluster_id         = "${var.cluster_id}"
  task_role_arn      = "${module.ecs_sierra_to_dynamo.task_role_arn}"
  vpc_id             = "${var.vpc_id}"
  app_uri            = "${var.ecr_repository_url}:${var.release_id}"
  nginx_uri          = "${var.ecr_repository_url}:${var.release_id}"
  listener_https_arn = "${var.alb_listener_https_arn}"
  listener_http_arn  = "${var.alb_listener_http_arn}"
  path_pattern       = "/sierra_to_dynamo/${var.resource_type}/*"
  alb_priority       = "100"
  healthcheck_path   = "/sierra_to_dynamo/${var.resource_type}/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/sierra_to_dynamo_${var.resource_type}.ini"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    queue_arn         = "${module.id_minter_topic.arn}"
    metrics_namespace = "sierra_to_dynamo-${var.resource_type}"
  }

  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"
  server_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"
}
