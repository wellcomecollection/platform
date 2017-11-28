module "loris" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=template-agnostic-services"
  name               = "loris"
  cluster_id         = "${aws_ecs_cluster.loris.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${local.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.loris_alb.listener_https_arn}"
  listener_http_arn  = "${module.loris_alb.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "100"

  cpu    = 3960
  memory = 7350

  desired_count = 4

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.loris_userdata.efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${module.loris_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}
