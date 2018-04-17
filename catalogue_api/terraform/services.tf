module "api_romulus_v1" {
  source = "./api_service"

  name     = "romulus"
  prod_api = "${var.production_api}"

  es_config = "${var.es_config_romulus}"

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

  es_config = "${var.es_config_remus}"

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
