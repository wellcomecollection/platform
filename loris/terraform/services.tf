module "loris" {
  source             = "../terraform/services"
  name               = "loris"
  cluster_id         = "${data.terraform_remote_state.platform.ecs_cluster_api_id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${data.terraform_remote_state.platform.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${data.terraform_remote_state.platform.api_alb_listener_https_arn}"
  listener_http_arn  = "${data.terraform_remote_state.platform.api_alb_listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "109"
  host_name          = "iiif-origin.wellcomecollection.org"

  cpu    = 1792
  memory = 4096

  desired_count = 4

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${data.terraform_remote_state.platform.api_userdata_efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${data.terraform_remote_state.platform.api_alb_cloudwatch_id}"
  server_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_client_error_alarm_arn}"
}

module "loris-2" {
  source             = "../../terraform/services"
  name               = "loris-2"
  cluster_id         = "${aws_ecs_cluster.loris.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${data.terraform_remote_state.platform.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${data.terraform_remote_state.platform.api_alb_listener_https_arn}"
  listener_http_arn  = "${data.terraform_remote_state.platform.api_alb_listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "110"
  host_name          = "iiif-2.wellcomecollection.org"

  cpu    = 1792
  memory = 4096

  desired_count = 4

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${data.terraform_remote_state.platform.api_userdata_efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${data.terraform_remote_state.platform.api_alb_cloudwatch_id}"
  server_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_client_error_alarm_arn}"
}
