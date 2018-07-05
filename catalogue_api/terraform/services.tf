module "api_romulus_v1" {
  source = "./api_service"

  name     = "romulus"
  prod_api = "${var.production_api}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${var.es_config_romulus}"

  prod_api_release_id       = "${var.production_api == "romulus" ? var.pinned_romulus_api : var.pinned_remus_api}"
  prod_api_nginx_release_id = "${var.production_api == "romulus" ? var.pinned_romulus_api_nginx : var.pinned_remus_api_nginx}"
  release_ids               = "${var.release_ids}"

  cluster_id = "${aws_ecs_cluster.api.id}"
  vpc_id     = "${module.vpc_api.vpc_id}"

  ecr_repository_api_url   = "${module.ecr_repository_api.repository_url}"
  ecr_repository_nginx_url = "${module.ecr_repository_nginx_api.repository_url}"

  api_prod_host  = "${var.api_prod_host}"
  api_stage_host = "${var.api_stage_host}"

  alb_listener_https_arn     = "${module.api_alb.listener_https_arn}"
  alb_listener_http_arn      = "${module.api_alb.listener_http_arn}"
  alb_cloudwatch_id          = "${module.api_alb.cloudwatch_id}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}

module "api_remus_v1" {
  source = "./api_service"

  name     = "remus"
  prod_api = "${var.production_api}"

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${var.es_config_remus}"

  prod_api_release_id       = "${var.production_api == "romulus" ? var.pinned_romulus_api : var.pinned_remus_api}"
  prod_api_nginx_release_id = "${var.production_api == "romulus" ? var.pinned_romulus_api_nginx : var.pinned_remus_api_nginx}"
  release_ids               = "${var.release_ids}"

  cluster_id = "${aws_ecs_cluster.api.id}"
  vpc_id     = "${module.vpc_api.vpc_id}"

  ecr_repository_api_url   = "${module.ecr_repository_api.repository_url}"
  ecr_repository_nginx_url = "${module.ecr_repository_nginx_api.repository_url}"

  api_prod_host  = "${var.api_prod_host}"
  api_stage_host = "${var.api_stage_host}"

  alb_listener_https_arn     = "${module.api_alb.listener_https_arn}"
  alb_listener_http_arn      = "${module.api_alb.listener_http_arn}"
  alb_cloudwatch_id          = "${module.api_alb.cloudwatch_id}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}

// api-delta

locals {
  romulus_api_is_pinned   = "${var.pinned_romulus_api == "" ? "false" : "true" }"
  romulus_nginx_is_pinned = "${var.pinned_romulus_api_nginx == "" ? "false" : "true" }"

  remus_api_is_pinned   = "${var.pinned_remus_api == "" ? "false" : "true" }"
  remus_nginx_is_pinned = "${var.pinned_remus_api_nginx == "" ? "false" : "true" }"

  romulus_api_release_id   = "${local.romulus_api_is_pinned == "true" ? var.pinned_romulus_api : var.release_ids["api"]}"
  romulus_nginx_release_id = "${local.romulus_nginx_is_pinned == "true" ? var.pinned_romulus_api_nginx : var.release_ids["nginx_api"]}"

  remus_api_release_id   = "${local.remus_api_is_pinned == "true" ? var.pinned_romulus_api : var.release_ids["api"]}"
  remus_nginx_release_id = "${local.remus_nginx_is_pinned == "true" ? var.pinned_romulus_api_nginx : var.release_ids["nginx_api"]}"

  romulus_app_uri   = "${module.ecr_repository_api.repository_url}:${local.romulus_api_release_id}"
  romulus_nginx_uri = "${module.ecr_repository_nginx_api.repository_url}:${local.romulus_nginx_release_id}"

  remus_app_uri   = "${module.ecr_repository_api.repository_url}:${local.remus_api_release_id}"
  remus_nginx_uri = "${module.ecr_repository_nginx_api.repository_url}:${local.remus_nginx_release_id}"

  romulus_is_prod = "${var.production_api == "romulus" ? "true" : "false"}"
  remus_is_prod   = "${var.production_api == "remus" ? "true" : "false"}"

  remus_hostname   = "${local.remus_is_prod == "true" ? var.api_prod_host : var.api_stage_host}"
  romulus_hostname = "${local.romulus_is_prod == "true" ? var.api_prod_host : var.api_stage_host}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${local.namespace}"
  vpc  = "${local.vpc_id}"
}

resource "aws_ecs_cluster" "cluster" {
  name = "${local.namespace}"
}

module "load_balancer" {
  source = "load_balancer"

  name = "${local.namespace}"

  vpc_id         = "${local.vpc_id}"
  public_subnets = "${local.public_subnets}"

  default_target_group_arn = "${module.api_romulus_delta.target_group_arn}"
  certificate_domain       = "api.wellcomecollection.org"

  service_lb_security_group_ids = [
    "${module.api_romulus_delta.service_lb_security_group_id}",
    "${module.api_remus_delta.service_lb_security_group_id}",
  ]
}

module "api_romulus_delta" {
  source = "service"

  name            = "${local.namespace}-romulus"
  cluster_id      = "${aws_ecs_cluster.cluster.id}"
  aws_region      = "${var.aws_region}"
  vpc_id          = "${local.vpc_id}"
  namespace_id    = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  private_subnets = "${local.private_subnets}"

  alb_id           = "${module.load_balancer.id}"
  alb_listener_arn = "${module.load_balancer.https_listener_arn}"

  sidecar_container_image = "${local.romulus_nginx_uri}"
  app_container_image     = "${local.romulus_app_uri}"

  host_name = "${local.romulus_hostname}"
}

module "api_remus_delta" {
  source = "service"

  name            = "${local.namespace}-remus"
  cluster_id      = "${aws_ecs_cluster.cluster.id}"
  aws_region      = "${var.aws_region}"
  vpc_id          = "${local.vpc_id}"
  namespace_id    = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  private_subnets = "${local.private_subnets}"

  alb_id           = "${module.load_balancer.id}"
  alb_listener_arn = "${module.load_balancer.https_listener_arn}"

  sidecar_container_image = "${local.remus_nginx_uri}"
  app_container_image     = "${local.remus_app_uri}"

  host_name = "${local.remus_hostname}"
}
