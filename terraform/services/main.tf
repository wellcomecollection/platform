module "service" {
  source              = "./ecs_service"
  service_name        = "${var.service_name}"
  cluster_id          = "${var.cluster_id}"
  task_definition_arn = "${module.task.arn}"
  alb_id              = "${var.alb_id}"
  vpc_id              = "${var.vpc_id}"
  acm_cert_arn        = "${var.cert_arn}"
  container_name      = "${var.container_name}"
  container_port      = "${var.container_port}"
}

module "task" {
  source           = "./ecs_tasks"
  task_name        = "${var.task_name}"
  task_role_arn    = "${var.task_role_arn}"
  volume_name      = "${var.volume_name}"
  volume_host_path = "${var.volume_host_path}"
  image_uri        = "${var.image_uri}"
  template_name    = "${var.template_name}"
}
