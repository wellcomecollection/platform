module "catalogue_api" {
  source = "service"

  namespace = "${var.namespace}"

  container_image = "${var.container_image}"
  container_port  = "${var.internal_port}"

  namespace_id = "${var.namespace_id}"
  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  security_group_ids = [
    "${aws_security_group.service_lb_ingress_security_group.id}",
    "${aws_security_group.interservice_security_group.id}"
  ]
  service_egress_security_group_id = "${aws_security_group.service_egress_security_group.id}"

  subnets = ["${var.subnets}"]

  es_cluster_credentials = "${var.es_cluster_credentials}"
  es_config              = "${var.es_config}"
}

