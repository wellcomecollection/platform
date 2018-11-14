module "services" {
  source = "services"

  namespace = "${var.namespace}"

  namespace_id  = "${var.namespace_id}"
  namespace_tld = "${var.namespace_tld}"

  subnets      = ["${var.subnets}"]
  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"
  nlb_arn      = "${module.nlb.arn}"

  container_port = "${var.container_port}"

  es_cluster_credentials = "${var.es_cluster_credentials}"

  remus_container_image = "${var.remus_container_image}"
  remus_es_config       = "${var.remus_es_config}"
  remus_listener_port   = "${local.remus_listener_port}"

  romulus_container_image = "${var.romulus_container_image}"
  romulus_es_config       = "${var.romulus_es_config}"
  romulus_listener_port   = "${local.romulus_listener_port}"

  nginx_container_image = "${var.nginx_container_image}"
  nginx_container_port  = "${var.nginx_container_port}"
}
