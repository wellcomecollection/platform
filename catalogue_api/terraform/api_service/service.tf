data "template_file" "es_cluster_host" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config["name"]}"
    region = "${var.es_config["region"]}"
  }
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/service?ref=v6.0.0"

  name = "api_${var.name}_v1"

  cluster_id = "${var.cluster_id}"
  vpc_id     = "${var.vpc_id}"

  app_uri   = "${var.ecr_repository_api_url}:${local.api_release_id}"
  nginx_uri = "${var.ecr_repository_nginx_url}:${local.nginx_release_id}"

  host_name = "${local.host_name}"

  enable_alb_alarm = "${local.enable_alb_alarm}"

  desired_count                      = "${local.desired_count}"
  deployment_minimum_healthy_percent = "${local.minimum_healthy_percent}"
  deployment_maximum_percent         = "200"

  cpu    = 1024
  memory = 2048

  env_vars = {
    api_host    = "${var.api_host}"
    es_host     = "${data.template_file.es_cluster_host.rendered}"
    es_port     = "${var.es_config["port"]}"
    es_name     = "${var.es_config["name"]}"
    es_index    = "${var.es_config["index"]}"
    es_doc_type = "${var.es_config["doc_type"]}"
    es_username = "${var.es_config["username"]}"
    es_password = "${var.es_config["password"]}"
    es_protocol = "${var.es_config["protocol"]}"
  }

  env_vars_length = 9

  listener_https_arn = "${var.alb_listener_https_arn}"
  listener_http_arn  = "${var.alb_listener_http_arn}"
  healthcheck_path   = "/management/healthcheck"
  path_pattern       = "/catalogue/v1/*"

  loadbalancer_cloudwatch_id   = "${var.alb_cloudwatch_id}"
  server_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"
}
