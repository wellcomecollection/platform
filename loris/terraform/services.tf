module "loris" {
  source             = "git::https://github.com/wellcometrust/terraform.git//ecs/service?ref=v6.1.1"
  name               = "loris"
  cluster_id         = "${aws_ecs_cluster.loris.id}"
  vpc_id             = "${local.vpc_api_id}"
  app_uri            = "wellcome/loris:latest"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.loris_alb.listener_https_arn}"
  listener_http_arn  = "${module.loris_alb.listener_http_arn}"

  path_pattern     = "/*"
  healthcheck_path = "/image/"

  cpu    = 3960
  memory = 7350

  desired_count = 4

  env_vars_length = 0

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.loris_userdata.mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${module.loris_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}
