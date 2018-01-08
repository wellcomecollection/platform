module "api_romulus_v1" {
  source = "./api_service"

  name     = "romulus"
  prod_api = "remus"

  es_config = "${var.es_config_romulus}"

  prod_api_release_id       = "${var.production_api == "romulus" ? var.pinned_romulus_api : var.pinned_remus_api}"
  prod_api_nginx_release_id = "${var.production_api == "romulus" ? var.pinned_romulus_api_nginx : var.pinned_remus_api_nginx}"
  release_ids               = "${var.release_ids}"

  cluster_id = "${aws_ecs_cluster.api.id}"
  vpc_id     = "${module.vpc_api.vpc_id}"

  ecr_repository_api_url   = "${module.ecr_repository_api.repository_url}"
  ecr_repository_nginx_url = "${module.ecr_repository_nginx_api.repository_url}"

  api_host = "${var.api_host}"

  alb_listener_https_arn     = "${module.api_alb.listener_https_arn}"
  alb_listener_http_arn      = "${module.api_alb.listener_http_arn}"
  alb_cloudwatch_id          = "${module.api_alb.cloudwatch_id}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}

data "template_file" "es_cluster_host_remus" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_remus["name"]}"
    region = "${var.es_config_remus["region"]}"
  }
}

module "api_remus_v1" {
  source             = "git::https://github.com/wellcometrust/terraform.git//service?ref=v5.0.2"
  name               = "api_remus_v1"
  cluster_id         = "${aws_ecs_cluster.api.id}"
  vpc_id             = "${module.vpc_api.vpc_id}"
  app_uri            = "${module.ecr_repository_api.repository_url}:${var.pinned_remus_api != "" ? var.pinned_remus_api : var.release_ids["api"]}"
  nginx_uri          = "${module.ecr_repository_nginx_api.repository_url}:${var.pinned_remus_api_nginx != "" ? var.pinned_remus_api_nginx : var.release_ids["nginx_api"]}"
  listener_https_arn = "${module.api_alb.listener_https_arn}"
  listener_http_arn  = "${module.api_alb.listener_http_arn}"

  healthcheck_path = "/management/healthcheck"

  path_pattern = "/catalogue/v1/*"
  alb_priority = "113"
  host_name    = "${var.production_api == "remus" ? var.api_host : var.api_host_stage}"

  enable_alb_alarm = "${var.production_api == "remus" ? 1 : 0}"

  cpu    = 1024
  memory = 2048

  desired_count = "${var.production_api == "remus" ? var.api_task_count : var.api_task_count_stage}"

  deployment_minimum_healthy_percent = "${var.production_api == "remus" ? "50" : "0"}"
  deployment_maximum_percent         = "200"

  env_vars = {
    api_host    = "${var.api_host}"
    es_host     = "${data.template_file.es_cluster_host_remus.rendered}"
    es_port     = "${var.es_config_remus["port"]}"
    es_name     = "${var.es_config_remus["name"]}"
    es_index    = "${var.es_config_remus["index"]}"
    es_doc_type = "${var.es_config_remus["doc_type"]}"
    es_username = "${var.es_config_remus["username"]}"
    es_password = "${var.es_config_remus["password"]}"
    es_protocol = "${var.es_config_remus["protocol"]}"
  }

  env_vars_length = 9

  loadbalancer_cloudwatch_id   = "${module.api_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}
