module "romulus" {
  source = "service"

  name            = "${local.namespace}-romulus"
  cluster_id      = "${aws_ecs_cluster.cluster.id}"
  aws_region      = "${var.aws_region}"
  vpc_id          = "${local.vpc_id}"
  namespace_id    = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  private_subnets = "${local.private_subnets}"

  alb_listener_arn_https = "${local.alb_api_wc_https_listener_arn}"

  sidecar_container_image = "${local.romulus_nginx_uri}"
  app_container_image     = "${local.romulus_app_uri}"

  host_name = "${local.romulus_hostname}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${local.es_config_romulus}"

  task_desired_count         = "${local.romulus_task_number}"
  enable_alb_alarm           = "${local.romulus_enable_alb_alarm}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${local.alb_api_wc_cloudwatch_id}"

  lb_service_security_group_id = "${local.alb_api_wc_service_lb_security_group_id}"
}

module "remus" {
  source = "service"

  name            = "${local.namespace}-remus"
  cluster_id      = "${aws_ecs_cluster.cluster.id}"
  aws_region      = "${var.aws_region}"
  vpc_id          = "${local.vpc_id}"
  namespace_id    = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  private_subnets = "${local.private_subnets}"

  alb_listener_arn_https = "${local.alb_api_wc_https_listener_arn}"

  sidecar_container_image = "${local.remus_nginx_uri}"
  app_container_image     = "${local.remus_app_uri}"

  host_name = "${local.remus_hostname}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${local.es_config_remus}"

  task_desired_count         = "${local.remus_task_number}"
  alb_cloudwatch_id          = "${local.alb_api_wc_cloudwatch_id}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  enable_alb_alarm           = "${local.remus_enable_alb_alarm}"

  lb_service_security_group_id = "${local.alb_api_wc_service_lb_security_group_id}"
}
