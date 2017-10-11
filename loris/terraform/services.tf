module "loris" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
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

  cpu    = 1792
  memory = 4096

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

module "loris_ebs" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "loris-ebs"
  cluster_id         = "${aws_ecs_cluster.loris_ebs.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${local.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.loris_alb_ebs.listener_https_arn}"
  listener_http_arn  = "${module.loris_alb_ebs.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris-ebs.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "101"

  cpu    = 2560
  memory = 3276

  desired_count = 4

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.loris_userdata_ebs.efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${module.loris_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}

module "loris_ebs_large" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "loris-ebs-large"
  cluster_id         = "${aws_ecs_cluster.loris_ebs_large.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${local.vpc_api_id}"
  app_uri            = "${module.ecr_loris.repository_url}:${var.release_ids["loris"]}"
  nginx_uri          = "${module.ecr_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.loris_alb_ebs_large.listener_https_arn}"
  listener_http_arn  = "${module.loris_alb_ebs_large.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris-ebs-large.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "101"

  cpu    = 1920
  memory = 3276

  desired_count = 4

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.loris_userdata_ebs_large.efs_mount_directory}/loris"
  container_path   = "/mnt/loris"

  loadbalancer_cloudwatch_id   = "${module.loris_alb_ebs_large.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}