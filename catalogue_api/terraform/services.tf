data "template_file" "es_cluster_host_romulus" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_romulus["name"]}"
    region = "${var.es_config_romulus["region"]}"
  }
}

module "api_romulus_v1" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=template-agnostic-services"
  name               = "api_romulus_v1"
  cluster_id         = "${aws_ecs_cluster.api.id}"
  task_role_arn      = "${module.ecs_api_iam.task_role_arn}"
  vpc_id             = "${module.vpc_api.vpc_id}"
  app_uri            = "${module.ecr_repository_api.repository_url}:${var.pinned_romulus_api != "" ? var.pinned_romulus_api : var.release_ids["api"]}"
  nginx_uri          = "${module.ecr_repository_nginx_api.repository_url}:${var.pinned_romulus_api_nginx != "" ? var.pinned_romulus_api_nginx : var.release_ids["nginx_api"]}"
  listener_https_arn = "${module.api_alb.listener_https_arn}"
  listener_http_arn  = "${module.api_alb.listener_http_arn}"
  infra_bucket       = "${local.bucket_infra_id}"

  config_key           = "config/${var.build_env}/api_romulus_v1.ini"
  config_template_path = "config/api_romulus_v1.ini.template"

  path_pattern = "/catalogue/v1/*"
  alb_priority = "114"
  host_name    = "${var.production_api == "romulus" ? var.api_host : var.api_host_stage}"

  enable_alb_alarm = "${var.production_api == "romulus" ? 1 : 0}"

  cpu    = 1024
  memory = 2048

  desired_count = "${var.production_api == "romulus" ? var.api_task_count : var.api_task_count_stage}"

  deployment_minimum_healthy_percent = "${var.production_api == "romulus" ? "50" : "0"}"
  deployment_maximum_percent         = "200"

  config_vars = {
    api_host    = "${var.api_host}"
    es_host     = "${data.template_file.es_cluster_host_romulus.rendered}"
    es_port     = "${var.es_config_romulus["port"]}"
    es_name     = "${var.es_config_romulus["name"]}"
    es_index    = "${var.es_config_romulus["index"]}"
    es_doc_type = "${var.es_config_romulus["doc_type"]}"
    es_username = "${var.es_config_romulus["username"]}"
    es_password = "${var.es_config_romulus["password"]}"
    es_protocol = "${var.es_config_romulus["protocol"]}"
  }

  loadbalancer_cloudwatch_id   = "${module.api_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}

data "template_file" "es_cluster_host_remus" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_remus["name"]}"
    region = "${var.es_config_remus["region"]}"
  }
}

module "api_remus_v1" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=template-agnostic-services"
  name               = "api_remus_v1"
  cluster_id         = "${aws_ecs_cluster.api.id}"
  task_role_arn      = "${module.ecs_api_iam.task_role_arn}"
  vpc_id             = "${module.vpc_api.vpc_id}"
  app_uri            = "${module.ecr_repository_api.repository_url}:${var.pinned_remus_api != "" ? var.pinned_remus_api : var.release_ids["api"]}"
  nginx_uri          = "${module.ecr_repository_nginx_api.repository_url}:${var.pinned_remus_api_nginx != "" ? var.pinned_remus_api_nginx : var.release_ids["nginx_api"]}"
  listener_https_arn = "${module.api_alb.listener_https_arn}"
  listener_http_arn  = "${module.api_alb.listener_http_arn}"
  infra_bucket       = "${local.bucket_infra_id}"

  config_key           = "config/${var.build_env}/api_remus_v1.ini"
  config_template_path = "config/api_remus_v1.ini.template"

  path_pattern = "/catalogue/v1/*"
  alb_priority = "113"
  host_name    = "${var.production_api == "remus" ? var.api_host : var.api_host_stage}"

  enable_alb_alarm = "${var.production_api == "remus" ? 1 : 0}"

  cpu    = 1024
  memory = 2048

  desired_count = "${var.production_api == "remus" ? var.api_task_count : var.api_task_count_stage}"

  deployment_minimum_healthy_percent = "${var.production_api == "remus" ? "50" : "0"}"
  deployment_maximum_percent         = "200"

  config_vars = {
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

  loadbalancer_cloudwatch_id   = "${module.api_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}
