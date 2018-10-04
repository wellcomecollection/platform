data "template_file" "es_cluster_host" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_cluster_credentials["name"]}"
    region = "${var.es_cluster_credentials["region"]}"
  }
}

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/container_with_sidecar?ref=v11.4.1"

  aws_region = "${var.aws_region}"
  task_name  = "${var.name}"

  cpu    = "${var.cpu}"
  memory = "${var.memory}"

  log_group_prefix = "${var.log_group_prefix}"

  app_container_image = "${var.app_container_image}"
  app_container_port  = "${var.app_container_port}"

  app_cpu    = "${var.app_cpu}"
  app_memory = "${var.app_memory}"

  app_env_vars = {
    api_host    = "api.wellcomecollection.org"
    es_host     = "${data.template_file.es_cluster_host.rendered}"
    es_port     = "${var.es_cluster_credentials["port"]}"
    es_username = "${var.es_cluster_credentials["username"]}"
    es_password = "${var.es_cluster_credentials["password"]}"
    es_protocol = "${var.es_cluster_credentials["protocol"]}"
    es_index_v1 = "${var.es_config["index_v1"]}"
    es_index_v2 = "${var.es_config["index_v2"]}"
    es_doc_type = "${var.es_config["doc_type"]}"
  }

  app_env_vars_length = 9

  sidecar_container_image = "${var.sidecar_container_image}"
  sidecar_container_port  = "${var.sidecar_container_port}"

  sidecar_cpu      = "${var.sidecar_cpu}"
  sidecar_memory   = "${var.sidecar_memory}"
  sidecar_env_vars = "${var.sidecar_env_vars}"

  sidecar_is_proxy = "true"
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=v11.6.0"

  service_name       = "${var.name}"
  task_desired_count = "${var.task_desired_count}"

  security_group_ids = [
    "${aws_security_group.service_egress_security_group.id}",
    "${var.lb_service_security_group_id}",
  ]

  deployment_minimum_healthy_percent = "${var.deployment_minimum_healthy_percent}"
  deployment_maximum_percent         = "200"

  ecs_cluster_id = "${var.cluster_id}"

  vpc_id = "${var.vpc_id}"

  subnets = [
    "${var.private_subnets}",
  ]

  namespace_id = "${var.namespace_id}"

  container_port = "${var.sidecar_container_port}"
  container_name = "${module.task.sidecar_task_name}"

  task_definition_arn = "${module.task.task_definition_arn}"

  healthcheck_path = "${var.healthcheck_path}"

  launch_type = "FARGATE"
}
