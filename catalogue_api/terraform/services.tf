locals {
  romulus_api_is_pinned   = "${var.pinned_romulus_api == "" ? "false" : "true" }"
  romulus_nginx_is_pinned = "${var.pinned_romulus_api_nginx-delta == "" ? "false" : "true" }"

  remus_api_is_pinned   = "${var.pinned_remus_api == "" ? "false" : "true" }"
  remus_nginx_is_pinned = "${var.pinned_remus_api_nginx-delta == "" ? "false" : "true" }"

  romulus_api_release_id   = "${local.romulus_api_is_pinned == "true" ? var.pinned_romulus_api : var.release_ids["api"]}"
  romulus_nginx_release_id = "${local.romulus_nginx_is_pinned == "true" ? var.pinned_romulus_api_nginx-delta : var.release_ids["nginx_api-delta"]}"

  remus_api_release_id   = "${local.remus_api_is_pinned == "true" ? var.pinned_remus_api : var.release_ids["api"]}"
  remus_nginx_release_id = "${local.remus_nginx_is_pinned == "true" ? var.pinned_remus_api_nginx-delta : var.release_ids["nginx_api-delta"]}"

  romulus_app_uri   = "${module.ecr_repository_api.repository_url}:${local.romulus_api_release_id}"
  romulus_nginx_uri = "${module.ecr_repository_nginx_api-delta.repository_url}:${local.romulus_nginx_release_id}"

  remus_app_uri   = "${module.ecr_repository_api.repository_url}:${local.remus_api_release_id}"
  remus_nginx_uri = "${module.ecr_repository_nginx_api-delta.repository_url}:${local.remus_nginx_release_id}"

  romulus_is_prod = "${var.production_api == "romulus" ? "true" : "false"}"
  remus_is_prod   = "${var.production_api == "remus" ? "true" : "false"}"

  remus_hostname   = "${local.remus_is_prod == "true" ? var.api_prod_host : var.api_stage_host}"
  romulus_hostname = "${local.romulus_is_prod == "true" ? var.api_prod_host : var.api_stage_host}"

  remus_task_number   = "${local.remus_is_prod == "true" ? 3 : 1}"
  romulus_task_number = "${local.romulus_is_prod == "true" ? 3 : 1}"

  remus_enable_alb_alarm   = "${local.remus_is_prod == "true" ? 1 : 0}"
  romulus_enable_alb_alarm = "${local.romulus_is_prod == "true" ? 1 : 0}"
}

module "api_romulus_delta" {
  source = "service"

  name            = "${local.namespace}-romulus"
  cluster_id      = "${aws_ecs_cluster.cluster.id}"
  aws_region      = "${var.aws_region}"
  vpc_id          = "${local.vpc_id}"
  namespace_id    = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  private_subnets = "${local.private_subnets}"

  alb_id                 = "${module.load_balancer.id}"
  alb_listener_arn_https = "${module.load_balancer.https_listener_arn}"
  alb_listener_arn_http  = "${module.load_balancer.http_listener_arn}"

  sidecar_container_image = "${local.romulus_nginx_uri}"
  app_container_image     = "${local.romulus_app_uri}"

  host_name = "${local.romulus_hostname}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${var.es_config_romulus}"

  task_desired_count         = "${local.romulus_task_number}"
  enable_alb_alarm           = "${local.romulus_enable_alb_alarm}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.load_balancer.cloudwatch_id}"
}

module "api_remus_delta" {
  source = "service"

  name            = "${local.namespace}-remus"
  cluster_id      = "${aws_ecs_cluster.cluster.id}"
  aws_region      = "${var.aws_region}"
  vpc_id          = "${local.vpc_id}"
  namespace_id    = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  private_subnets = "${local.private_subnets}"

  alb_id                 = "${module.load_balancer.id}"
  alb_listener_arn_https = "${module.load_balancer.https_listener_arn}"
  alb_listener_arn_http  = "${module.load_balancer.http_listener_arn}"

  sidecar_container_image = "${local.remus_nginx_uri}"
  app_container_image     = "${local.remus_app_uri}"

  host_name = "${local.remus_hostname}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${var.es_config_remus}"

  task_desired_count         = "${local.remus_task_number}"
  alb_cloudwatch_id          = "${module.load_balancer.cloudwatch_id}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  enable_alb_alarm           = "${local.remus_enable_alb_alarm}"
}
