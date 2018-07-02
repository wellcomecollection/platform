locals {
  is_prod_api = "${var.name == var.prod_api ? true : false}"

  api_release_id   = "${local.is_prod_api ? var.prod_api_release_id : var.release_ids["api"]}"
  nginx_release_id = "${local.is_prod_api ? var.prod_api_nginx_release_id : var.release_ids["nginx_api"]}"

  host_name = "${local.is_prod_api ? var.api_prod_host : var.api_stage_host}"
}

data "template_file" "es_cluster_host" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_cluster_credentials["name"]}"
    region = "${var.es_cluster_credentials["region"]}"
  }
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/service?ref=v10.3.0"

  name = "api_${var.name}_v1"

  cluster_id = "${var.cluster_id}"
  vpc_id     = "${var.vpc_id}"

  app_uri   = "${var.ecr_repository_api_url}:${local.api_release_id}"
  nginx_uri = "${var.ecr_repository_nginx_url}:${local.nginx_release_id}"

  host_name = "${local.host_name}"

  enable_alb_alarm = "${local.is_prod_api ? 1 : 0}"

  desired_count                      = "${local.is_prod_api ? 3 : 1}"
  deployment_minimum_healthy_percent = "${local.is_prod_api ? 50 : 0}"
  deployment_maximum_percent         = "200"

  cpu    = 1024
  memory = 2048

  env_vars = {
    api_host    = "${local.host_name}"
    es_host     = "${data.template_file.es_cluster_host.rendered}"
    es_port     = "${var.es_cluster_credentials["port"]}"
    es_username = "${var.es_cluster_credentials["username"]}"
    es_password = "${var.es_cluster_credentials["password"]}"
    es_protocol = "${var.es_cluster_credentials["protocol"]}"
    es_index_v1 = "v1-${var.es_config["index_suffix"]}"
    es_index_v2 = "v2-${var.es_config["index_suffix"]}"
    es_doc_type = "${var.es_config["doc_type"]}"
  }

  listener_https_arn = "${var.alb_listener_https_arn}"
  listener_http_arn  = "${var.alb_listener_http_arn}"
  healthcheck_path   = "/management/healthcheck"
  path_pattern       = "/catalogue/*"

  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"
  server_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"

  log_retention_in_days = 90
}
