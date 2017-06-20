module "service-tasks"{
  source = "../service-tasks"
  name        = "${var.name}"
  task_role_arn    = "${var.task_role_arn}"
  volume_name      = "${var.volume_name}"
  volume_host_path = "${var.volume_host_path}"
  vpc_id              = "${var.vpc_id}"
  container_name      = "${var.container_name}"
  container_port      = "${var.container_port}"
  listener_arn        = "${var.listener_arn}"
  path_pattern        = "${var.path_pattern}"
  alb_priority        = "${var.alb_priority}"
  desired_count       = "${var.desired_count}"
  healthcheck_path    = "${var.healthcheck_path}"
  cluster_id          = "${var.cluster_id}"
  container_definitions = "${module.container_definition.rendered}"
}

module "container_definition"{
  source = "../container_definitions"
  name = "${var.name}"
  volume_name      = "${var.volume_name}"
  app_uri          = "${var.app_uri}"
  nginx_uri        = "${var.nginx_uri}"
  template_name    = "${var.template_name}"
  infra_bucket     = "${var.infra_bucket}"
  config_key       = "${var.config_key}"
}

module "config" {
  source        = "./config"
  app_name      = "${var.name}"
  infra_bucket  = "${var.infra_bucket}"
  config_key    = "${var.config_key}"
  template_vars = "${var.config_vars}"
}
