data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}

module "service" {
  source = "git::github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=v16.1.3"

  service_name       = "${var.namespace}"
  task_desired_count = "${var.task_desired_count}"

  task_definition_arn = "${module.task.task_definition_arn}"

  security_group_ids = ["${local.security_group_ids}"]

  container_name = "sidecar"
  container_port = "${var.nginx_container_port}"

  ecs_cluster_id = "${data.aws_ecs_cluster.cluster.id}"

  vpc_id  = "${var.vpc_id}"
  subnets = "${var.subnets}"

  namespace_id = "${var.namespace_id}"

  launch_type           = "FARGATE"
  target_group_protocol = "TCP"

  listener_port = "${var.listener_port}"
  lb_arn        = "${var.lb_arn}"
}

module "task" {
  source = "git::github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/container_with_sidecar?ref=v19.6.0"

  cpu    = 1024
  memory = 2048

  launch_types = ["FARGATE"]

  app_cpu    = 512
  app_memory = 1024

  sidecar_cpu    = 512
  sidecar_memory = 1024

  app_env_vars = {
    api_host    = "api.wellcomecollection.org"
    es_index_v1 = "${var.es_config["index_v1"]}"
    es_index_v2 = "${var.es_config["index_v2"]}"
  }

  app_env_vars_length = 3

  sidecar_env_vars = {
    APP_HOST = "localhost"
    APP_PORT = "${var.container_port}"
  }

  sidecar_env_vars_length = 2

  secret_app_env_vars = {
    es_host     = "catalogue/api/es_host"
    es_port     = "catalogue/api/es_port"
    es_protocol = "catalogue/api/es_protocol"
    es_username = "catalogue/api/es_username"
    es_password = "catalogue/api/es_password"
  }

  secret_app_env_vars_length = 5

  aws_region = "eu-west-1"
  task_name  = "${var.namespace}"

  app_container_image = "${var.container_image}"
  app_container_port  = "${var.container_port}"

  sidecar_container_image = "${var.nginx_container_image}"
  sidecar_container_port  = "${var.nginx_container_port}"
}

locals {
  security_group_ids = "${concat(list(var.service_egress_security_group_id), var.security_group_ids)}"
}
