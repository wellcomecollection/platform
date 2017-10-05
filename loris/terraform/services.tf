module "loris" {
  source             = "../../terraform/services"
  name               = "loris"
  cluster_id         = "${aws_ecs_cluster.loris.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${data.terraform_remote_state.platform.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.loris_alb.listener_https_arn}"
  listener_http_arn  = "${module.loris_alb.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "100"

  cpu    = 1792
  memory = 4096

  desired_count = 4

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.loris_userdata.efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${module.loris_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_client_error_alarm_arn}"
}

module "loris_ebs" {
  source             = "../../terraform/services"
  name               = "loris-ebs"
  cluster_id         = "${aws_ecs_cluster.loris_ebs.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${data.terraform_remote_state.platform.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.loris_alb_ebs.listener_https_arn}"
  listener_http_arn  = "${module.loris_alb_ebs.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris-ebs.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "101"

  cpu    = 1792
  memory = 4096

  desired_count = 4

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.loris_userdata_ebs.efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${module.loris_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_client_error_alarm_arn}"
}

module "loris_m4" {
  source             = "../../terraform/services"
  name               = "loris-m4"
  cluster_id         = "${aws_ecs_cluster.loris_m4.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${data.terraform_remote_state.platform.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.loris_alb_m4.listener_https_arn}"
  listener_http_arn  = "${module.loris_alb_m4.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris-m4.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "101"

  cpu    = 2560
  memory = 3276

  desired_count = 2

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.loris_userdata_m4.efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${module.loris_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${data.terraform_remote_state.platform.alb_client_error_alarm_arn}"
}
