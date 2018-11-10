module "services" {
  source = "services"

  namespace = "${var.namespace}"

  namespace_id = "${var.namespace_id}"
  subnets      = ["${var.subnets}"]
  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"
  nlb_arn      = "${var.nlb_arn}"

  container_image = "${var.container_image}"
  container_port  = "${var.container_port}"

  es_cluster_credentials = "${var.es_cluster_credentials}"

  remus_es_config     = "${var.remus_es_config}"
  remus_listener_port = "${local.remus_listener_port}"

  romulus_es_config     = "${var.romulus_es_config}"
  romulus_listener_port = "${local.remus_listener_port}"

}
