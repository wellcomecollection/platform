module "grafana" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "grafana"
  cluster_id         = "${var.cluster_id}"
  task_role_arn      = "${module.ecs_grafana_iam.task_role_arn}"
  vpc_id             = "${var.vpc_id}"
  listener_https_arn = "${var.listener_https_arn}"
  listener_http_arn  = "${var.listener_http_arn}"

  cpu    = 256
  memory = 256

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  nginx_uri                = "${module.ecr_repository_nginx_grafana.repository_url}:${var.release_ids["nginx_grafana"]}"
  healthcheck_path         = "/api/health"
  secondary_container_port = "3000"
  app_uri                  = "grafana/grafana:4.4.3"

  volume_name      = "grafana"
  volume_host_path = "${var.efs_mount_directory}/grafana"
  container_path   = "/var/lib/grafana"

  extra_vars = [
    "{\"name\" : \"GF_AUTH_ANONYMOUS_ENABLED\", \"value\" : \"${var.grafana_anonymous_enabled}\"}",
    "{\"name\" : \"GF_AUTH_ANONYMOUS_ORG_ROLE\", \"value\" : \"${var.grafana_anonymous_role}\"}",
    "{\"name\" : \"GF_SECURITY_ADMIN_USER\", \"value\" : \"${var.grafana_admin_user}\"}",
    "{\"name\" : \"GF_SECURITY_ADMIN_PASSWORD\", \"value\" : \"${var.grafana_admin_password}\"}",
  ]

  config_key        = ""
  infra_bucket      = ""
  is_config_managed = false

  loadbalancer_cloudwatch_id   = "${var.cloudwatch_id}"
  server_error_alarm_topic_arn = "${var.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${var.alb_client_error_alarm_arn}"
}
