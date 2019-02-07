module "services" {
  source = "services"

  namespace = "${var.namespace}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  subnets      = ["${var.subnets}"]
  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"
  nlb_arn      = "${module.nlb.arn}"

  container_port = "${var.container_port}"

  remus_container_image = "${var.remus_container_image}"
  remus_es_config       = "${var.remus_es_config}"
  remus_listener_port   = "${local.remus_listener_port}"

  romulus_container_image = "${var.romulus_container_image}"
  romulus_es_config       = "${var.romulus_es_config}"
  romulus_listener_port   = "${local.romulus_listener_port}"

  v1_amber_container_image = "${var.v1_amber_container_image}"
  v1_amber_es_config       = "${var.v1_amber_es_config}"
  v1_amber_listener_port   = "${local.v1_amber_listener_port}"

  nginx_container_image = "${var.nginx_container_image}"
  nginx_container_port  = "${var.nginx_container_port}"

  remus_task_number    = "${var.remus_task_number}"
  romulus_task_number  = "${var.romulus_task_number}"
  v1_amber_task_number = "${var.v1_amber_task_number}"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.namespace}"
  vpc  = "${var.vpc_id}"
}
