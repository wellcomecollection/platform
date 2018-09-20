resource "aws_ecs_cluster" "cluster" {
  name = "${var.namespace}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}_api"
  vpc  = "${var.vpc_id}"
}

module "api_ecs" {
  source    = "service"
  namespace = "${var.namespace}_api"

  lb_listener_arn = "${aws_alb_listener.api_https.arn}"
  vpc_id          = "${var.vpc_id}"

  api_container_image = "${var.archive_api_container_image}"
  api_container_port  = "${var.archive_api_container_port}"

  api_env_vars = {
    TABLE_NAME = "${var.archive_progress_table_name}"
    TOPIC_ARN  = "${var.archive_ingest_sns_topic_arn}"
  }

  api_env_vars_length = 2

  nginx_container_image = "${var.nginx_container_image}"
  nginx_container_port  = "${var.nginx_container_port}"

  nginx_env_vars = {
    NGINX_PORT   = "${var.nginx_container_port}"
    HTTPS_DOMAIN = "api.wellcomecollection.org"
    HOST_PATH    = "${var.api_path}"
    APP_PORT     = "${var.archive_api_container_port}"
    APP_HOST     = "localhost"
  }

  nginx_env_vars_length = 5

  service_lb_security_group_id = "${aws_security_group.service_lb_security_group.id}"
  ecs_cluster_id               = "${aws_ecs_cluster.cluster.id}"
  subnets                      = "${var.private_subnets}"

  service_discovery_namespace = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  health_check_path           = "${var.api_path}/healthcheck"

  task_cpu     = "2048"
  task_memory  = "4096"
  api_cpu      = "1920"
  api_memory   = "3584"
  nginx_cpu    = "128"
  nginx_memory = "512"
}
