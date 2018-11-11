module "romulus" {
  source = "service"

  namespace = "${var.namespace}-romulus"

  container_image = "${var.romulus_container_image}"
  container_port  = "${var.container_port}"

  namespace_id = "${var.namespace_id}"
  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  security_group_ids = [
    "${aws_security_group.service_lb_ingress_security_group.id}",
    "${aws_security_group.interservice_security_group.id}",
  ]

  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"

  subnets = ["${var.subnets}"]

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${var.romulus_es_config}"
}

module "romulus_listener" {
  source = "listener"

  nlb_arn           = "${var.nlb_arn}"
  listener_port     = "${var.romulus_listener_port}"
  target_group_name = "${module.romulus.target_group_name}"
}
