module "service" {
  source              = "./ecs_service"
  service_name        = "${var.name}"
  cluster_id          = "${var.cluster_id}"
  task_definition_arn = "${module.task.arn}"
  vpc_id              = "${var.vpc_id}"
  container_name      = "${var.container_name}"
  container_port      = "${var.container_port}"
  listener_https_arn  = "${var.listener_https_arn}"
  listener_http_arn   = "${var.listener_http_arn}"
  path_pattern        = "${var.path_pattern}"
  alb_priority        = "${var.alb_priority}"
  desired_count       = "${var.desired_count}"
  healthcheck_path    = "${var.healthcheck_path}"
  infra_bucket        = "${var.infra_bucket}"
  host_name           = "${var.host_name}"
}

module "task" {
  source           = "./ecs_tasks"
  task_name        = "${var.name}"
  task_role_arn    = "${var.task_role_arn}"
  volume_name      = "${var.volume_name}"
  volume_host_path = "${var.volume_host_path}"
  app_uri          = "${var.app_uri}"
  nginx_uri        = "${var.nginx_uri}"
  template_name    = "${var.template_name}"
  infra_bucket     = "${var.infra_bucket}"
  config_key       = "${var.config_key}"

  docker_image     = "${var.docker_image}"
  container_port   = "${var.container_port}"
  container_path   = "${var.container_path}"
  environment_vars = "${var.environment_vars}"
}

module "config" {
  source            = "./config"
  app_name          = "${var.name}"
  infra_bucket      = "${var.infra_bucket}"
  config_key        = "${var.config_key}"
  template_vars     = "${var.config_vars}"
  is_config_managed = "${var.is_config_managed}"
}
